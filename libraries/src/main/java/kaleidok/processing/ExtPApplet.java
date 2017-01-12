package kaleidok.processing;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GLAutoDrawable;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.WritableNumberValue;
import javafx.geometry.Rectangle2D;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.newt.WindowSupport;
import kaleidok.newt.event.AbstractWindowListener;
import kaleidok.processing.event.KeyEventSupport;
import kaleidok.processing.event.KeyStroke;
import kaleidok.processing.export.ImageSaveSet;
import kaleidok.processing.image.ImageIO;
import kaleidok.processing.image.ImageResizeMode;
import kaleidok.processing.image.PImageFutures;
import kaleidok.processing.support.FrameRateSwitcherProperty;
import kaleidok.processing.support.GeometryPreferences;
import kaleidok.util.Arrays;
import kaleidok.util.Reflection;
import kaleidok.util.Strings;
import kaleidok.util.prefs.DefaultValueParser;
import kaleidok.util.Threads;
import kaleidok.util.concurrent.GroupedThreadFactory;
import org.apache.commons.lang3.StringUtils;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PSurface;
import processing.opengl.PGraphicsOpenGL;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;


/**
 * This is an intermediary class to "enhance" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private final ProcessingSketchApplication<? extends ExtPApplet> parent;

  public final Set<String> saveFilenames = new ImageSaveSet(this);

  protected ExecutorService executorService;

  private volatile CountDownLatch showSurfaceLatch = new CountDownLatch(1);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected final List<List<Map.Entry<KeyStroke, Consumer<? super KeyEvent>>>> keyEventHandlers =
    Arrays.asImmutableList(
      Stream.generate(() -> new ArrayList<>(0))
        .limit(3).toArray(List[]::new));

  private final GeometryPreferences geometryPreferences =
    new GeometryPreferences(this, true);

  private final FrameRateSwitcherProperty targetFrameRate;


  protected ExtPApplet()
  {
    this(null);
  }


  protected ExtPApplet( ProcessingSketchApplication<? extends ExtPApplet> parent )
  {
    this.parent = parent;
    documentBase = getDocumentBase(getClass(), parent);

    targetFrameRate = new FrameRateSwitcherProperty(this);
    targetFrameRate.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void settings()
  {
    parseAndSetConfig();

    if (executorService == null)
      executorService = makeExecutorService(getClass());
  }


  private void parseAndSetConfig()
  {
    if (parent != null && parent.getUnnamedBooleanParameter("fullscreen"))
    {
      fullScreen(sketchDisplay());
    }
    else if (!sketchFullScreen())
    {
      geometryPreferences.applySize();
    }

    Map<String, String> params = getParameterMap();
    smooth(DefaultValueParser.parseInt(
      params.get(sketchRenderer() + ".smooth"), sketchSmooth()));
  }


  static ExecutorService makeExecutorService( Class<?> namingClass )
  {
    return new ThreadPoolExecutor(
      0, 16, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new GroupedThreadFactory(
        Reflection.getAnonymousClassSimpleName(namingClass) + " worker pool",
        false, true));
  }


  @Override
  protected PSurface initSurface()
  {
    PSurface surface = super.initSurface();

    if (P3D.equals(sketchRenderer()))
    {
      final Runnable toggleFullScreenAction = this::toggleFullscreen;
      keyEventHandlers.get(processing.event.KeyEvent.TYPE - 1).add(
        new SimpleEntry<>(KeyStroke.fullscreenKeystroke,
          (ev) -> thread(toggleFullScreenAction)));

      if (parent != null)
      {
        /*
         * Invoke parent#exit() instead of PApplet#exit() when closing the
         * sketch window.
         */
        ((Window) surface.getNative()).addWindowListener(0,
          new AbstractWindowListener()
            {
              @Override
              public void windowDestroyNotify( WindowEvent ev )
              {
                parent.exit();
              }
            });
      }
    }

    Map<String, String> params = getParameterMap();
    String sResizable = params.get("resizable");
    if (sResizable != null)
      surface.setResizable(DefaultValueParser.parseBoolean(sResizable));

    return surface;
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  protected void showSurface()
  {
    geometryPreferences.applyPosition();
    geometryPreferences.bind();

    if (P3D.equals(sketchRenderer()))
    {
      ((Window) getSurface().getNative()).addWindowListener(
        new AbstractWindowListener()
        {
          @Override
          public void windowResized( WindowEvent ev )
          {
            Window w = (Window) ev.getSource();
            w.removeWindowListener(this);
            System.out.format("window resized (%d, %d)%n", w.getWidth(), w.getHeight()); // TODO: remove
            triggerShowSurface();
          }
        });

      super.showSurface();
    }
    else
    {
      super.showSurface();
      triggerShowSurface();
    }
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void setup()
  {
    targetFrameRate.setup();
  }


  @Override
  public void frameRate( float fps )
  {
    super.frameRate(fps);
    setTargetFrameRate(fps);
  }


  private void triggerShowSurface()
  {
    showSurfaceLatch.countDown();
    showSurfaceLatch = null;
  }


  public void awaitShowSurface() throws InterruptedException
  {
    CountDownLatch showSurfaceLatch = this.showSurfaceLatch;
    if (showSurfaceLatch != null)
      showSurfaceLatch.await();
  }


  @Override
  public void exitActual()
  {
    if (PConstants.P3D.equals(sketchRenderer()))
    {
      /*
       * PSurfaceJOGL starts a thread watching for exceptions thrown by the
       * animator thread. Unfortunately that thread isn't flagged as a daemon,
       * so it'll keep running beyond the termination of the animator thread.
       * However, with the following trick we can send a fake null exception as
       * a signal to that thread which leads to its voluntary termination.
       */
        ((GLAutoDrawable) getSurface().getNative()).getAnimator()
          .getUncaughtExceptionHandler().uncaughtException(null, null, null);
    }

    if (parent == null)
      super.exitActual();
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void dispose()
  {
    saveFilenames.clear();
    if (executorService != null)
      executorService.shutdownNow();
    super.dispose();
  }


  public Map<String, String> getParameterMap()
  {
    return (parent != null) ?
      parent.getNamedParameters() :
      Collections.emptyMap();
  }


  private final URL documentBase;


  public URL getDocumentBase()
  {
    return documentBase;
  }


  private static URL getDocumentBase( Class<?> codeLocationReference,
    Application parent )
  {
    // Work around for https://bugs.openjdk.java.net/browse/JDK-8160464
    HostServices services =
      (parent != null &&
        !System.getProperty("javafx.runtime.version", "").startsWith("8."))
      ?
        parent.getHostServices() :
        null;
    try
    {
      return
        (services != null && !services.getCodeBase().isEmpty()) ?
          new URL(services.getDocumentBase()) :
          new URL(
            codeLocationReference
              .getProtectionDomain().getCodeSource().getLocation(),
            "data/");
    }
    catch (MalformedURLException ex)
    {
      throw new InternalError("Invalid code or document base", ex);
    }
  }


  public Future<PImage> getImageFuture( String path )
  {
    URL url = this.getClass().getResource(path);
    if (url == null) {
      try {
        url = new URL(getDocumentBase(), path);
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(ex);
      }
    }
    return getImageFuture(url);
  }


  public Future<PImage> getImageFuture( URL url )
  {
    RunnableFuture<PImage> fImage = PImageFutures.from(url);
    if (!fImage.isDone())
      thread(fImage);
    return fImage;
  }


  @Override
  public void smooth( int level )
  {
    if (level > 0) {
      super.smooth(level);
    } else {
      noSmooth();
    }
  }


  public void image( PImage img, ImageResizeMode resizeMode,
    float a, float b, float c, float d )
  {
    switch (g.imageMode)
    {
    case CENTER:
      a -= c * 0.5f;
      b -= d * 0.5f;
      //noinspection fallthrough

    case CORNER:
      c += a;
      d += b;
      break;
    }

    resizeMode.drawImage(this, img, a, b, c, d);
  }


  public void save( String filename, boolean fullFrame )
  {
    if (fullFrame) {
      saveFilenames.add(filename);
    } else {
      save(filename);
    }
  }


  @Override
  public void save( final String filename )
  {
    loadPixels();
    thread(() ->
    {
      switch (g.format)
      {
      case RGB:
      case ARGB:
        if (Strings.endsWith(filename, ".bmp", true))
        {
          Path filePath = Paths.get(savePath(filename), EMPTY_STRING_ARRAY);
          try
          {
            ImageIO.saveBmp32(filePath, width, height, pixels, 0).force();
            break;
          }
          catch (UnsupportedOperationException ignored)
          {
            // try again with default code path
          }
          catch (IOException ex)
          {
            Threads.handleUncaught(ex);
            break;
          }
        }
        // fall through

      default:
        super.save(filename);
        break;
      }
    });
  }


  public void thread( Runnable action )
  {
    executorService.execute(action);
  }


  @Override
  public void thread( String methodName )
  {
    final Method method;
    try
    {
      method = getClass().getMethod(methodName, EMPTY_CLASS_ARRAY);
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalArgumentException(ex);
    }

    thread(() ->
    {
      try
      {
        method.invoke(this, EMPTY_OBJECT_ARRAY);
      }
      catch (IllegalAccessException | IllegalArgumentException ex)
      {
        throw new AssertionError(ex);
      }
      catch (InvocationTargetException ex)
      {
        Threads.handleUncaught(ex.getCause());
      }
    });
  }


  @Override
  protected void handleKeyEvent( processing.event.KeyEvent event )
  {
    List<Map.Entry<KeyStroke, Consumer<? super KeyEvent>>> handlers =
      keyEventHandlers.get(event.getAction() - 1);
    if (!handlers.isEmpty())
    {
      KeyEvent newtEvent = KeyEventSupport.convert(event);
      if (newtEvent != null)
      {
        for (Map.Entry<KeyStroke, Consumer<? super KeyEvent>> e: handlers)
        {
          if (e.getKey().matches(newtEvent))
          {
            e.getValue().accept(newtEvent);
            if (newtEvent.isConsumed())
              return;
          }
        }
      }
    }
    super.handleKeyEvent(event);
  }


  @Override
  public void keyPressed()
  {
    if (parent != null && key == ESC)
    {
      key = 0;
      parent.exit();
    }
  }


  public Rectangle2D getWindowBounds()
  {
    if (!P3D.equals(sketchRenderer()))
    {
      showMethodWarning("getWindowBounds");
      return null;
    }

    Window w = (Window) getSurface().getNative();
    Screen screen = w.getScreen();
    InsetsImmutable insets = w.getInsets();
    return new Rectangle2D(
      screen.getX() + w.getX() - insets.getLeftWidth(),
      screen.getY() + w.getY() - insets.getTopHeight(),
      w.getWidth() + insets.getTotalWidth(),
      w.getHeight() + insets.getTotalHeight());
  }


  public synchronized boolean toggleFullscreen()
  {
    if (!P3D.equals(sketchRenderer()))
    {
      showMethodWarning("toggleFullscreen");
      return sketchFullScreen();
    }

    Window w = (Window) getSurface().getNative();
    boolean oldFullscreenState = w.isFullscreen();
    boolean newFullScreenState = WindowSupport.toggleFullscreen(w);
    if (newFullScreenState == oldFullscreenState)
    {
      System.err.format(
        "Couldn't set fullscreen state to %b on %s.%n",
        !oldFullscreenState, w);
    }
    return newFullScreenState;
  }


  @Nullable
  private <T extends Number> List<T> parseParamNumberList( String key, int max,
    Function<? super String, T> valueParser )
  {
    if (key.isEmpty())
      throw new IllegalArgumentException("empty key");

    String value = getParameterMap().get(key);
    return (value != null) ? parseNumberList(value, max, valueParser) : null;
  }


  private boolean parseParamNumberList( String key,
    Function<? super String, ? extends Number> valueParser,
    WritableNumberValue... output )
  {
    List<? extends Number> result =
      parseParamNumberList(key, output.length, valueParser);
    if (result != null)
    {
      if (result.size() != output.length)
      {
        throw new IllegalArgumentException(
          output.length + " integers expected in \"" + key + '\"');
      }
      for (int i = output.length - 1; i >= 0; i--)
        output[i].setValue(result.get(i));
    }
    return result != null;
  }


  @Nullable
  private static <T extends Number> List<T> parseNumberList( String s, int max,
    Function<? super String, T> valueParser )
  {
    return parseNumberList(s, ",", max, valueParser);
  }


  @Nullable
  private static <T extends Number> List<T> parseNumberList( String s,
    String delimiter, int max, Function<? super String, T> valueParser )
  {
    if (s.isEmpty())
      return null;

    try
    {
      return Stream.of(StringUtils.split(s, delimiter, max))
        .map(valueParser)
        .collect(Collectors.toList());
    }
    catch (NumberFormatException ignored)
    {
      return Collections.emptyList();
    }
  }


  public boolean isDrawingThread()
  {
    if (g instanceof PGraphicsOpenGL)
      return ((PGraphicsOpenGL) g).pgl.threadIsCurrent();

    showMethodWarning("isDrawingThread");
    throw new UnsupportedOperationException();
  }


  public DoubleProperty targetFrameRateProperty()
  {
    return targetFrameRate;
  }

  public float getTargetFrameRate()
  {
    return (float) targetFrameRate.get();
  }

  public void setTargetFrameRate( float fps )
  {
    targetFrameRate.set(fps);
  }


  protected Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>
  getAppletPreferenceAdapters()
  {
    return Stream.<ReadOnlyPropertyPreferencesAdapter<?,?>>concat(
      geometryPreferences.getPreferenceAdapters(),
      Stream.of(targetFrameRate.getAspect(
        PropertyPreferencesAdapterTag.getInstance())));
  }


  protected Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return getAppletPreferenceAdapters();
  }


  protected void loadPreferences()
  {
    parseParamNumberList("size", Integer::valueOf,
      geometryPreferences.w, geometryPreferences.h);
    parseParamNumberList("location", Integer::valueOf,
      geometryPreferences.x, geometryPreferences.y);

    getAppletPreferenceAdapters()
      .forEach(ReadOnlyPropertyPreferencesAdapter::loadIfWritable);
  }


  protected void savePreferences()
  {
    ReadOnlyPropertyPreferencesAdapter.saveAndFlush(getPreferenceAdapters());
  }
}
