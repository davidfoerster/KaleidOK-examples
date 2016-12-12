package kaleidok.processing;

import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.GLAutoDrawable;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.property.DoubleProperty;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.newt.WindowSupport;
import kaleidok.processing.event.KeyEventSupport;
import kaleidok.processing.event.KeyStroke;
import kaleidok.processing.export.ImageSaveSet;
import kaleidok.processing.image.ImageIO;
import kaleidok.processing.image.PImageFutures;
import kaleidok.processing.support.FrameRateSwitcherProperty;
import kaleidok.util.Arrays;
import kaleidok.util.Reflection;
import kaleidok.util.prefs.DefaultValueParser;
import kaleidok.util.Threads;
import kaleidok.util.concurrent.GroupedThreadFactory;
import kaleidok.util.prefs.PreferenceUtils;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PSurface;
import processing.opengl.PGraphicsOpenGL;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static kaleidok.util.prefs.PreferenceUtils.getInt;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;
import static processing.event.KeyEvent.TYPE;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  public static final int MIN_DIMENSION = 50;


  private ProcessingSketchApplication<? extends ExtPApplet> parent;

  protected final Preferences preferences =
    Preferences.userNodeForPackage(this.getClass());

  public final Set<String> saveFilenames = new ImageSaveSet(this);

  protected ExecutorService executorService;

  private CountDownLatch showSurfaceLatch = new CountDownLatch(1);

  protected final List<List<Map.Entry<KeyStroke, Consumer<? super KeyEvent>>>> keyEventHandlers;

  {
    @SuppressWarnings("unchecked")
    List<Map.Entry<KeyStroke, Consumer<? super KeyEvent>>>[] keyEventHandlers = new List[3];
    for (int i = keyEventHandlers.length - 1; i >= 0; i--)
      keyEventHandlers[i] = new ArrayList<>(0);

    final Runnable toggleFullScreenAction = this::toggleFullscreen;
    keyEventHandlers[TYPE-1].add(new SimpleEntry<>(
      KeyStroke.fullscreenKeystroke, ( ev ) -> thread(toggleFullScreenAction)));

    this.keyEventHandlers = Arrays.asImmutableList(keyEventHandlers);
  }


  protected final String PREF_GEOMETRY =
    Reflection.getAnonymousClassSimpleName(getClass()) + ".geometry.";

  private final FrameRateSwitcherProperty targetFrameRate;


  public ExtPApplet( ProcessingSketchApplication<? extends ExtPApplet> parent )
  {
    this.parent = parent;
    documentBase = getDocumentBase(getClass(), parent);

    targetFrameRate = new FrameRateSwitcherProperty(this);
    targetFrameRate
      .addAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void settings()
  {
    parseAndSetConfig();
    executorService = new ThreadPoolExecutor(
      0, 16, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new GroupedThreadFactory(
        Reflection.getAnonymousClassSimpleName(getClass()) + " worker pool",
        false, true));
  }


  private void parseAndSetConfig()
  {
    if (parent.getUnnamedBooleanParameter("fullscreen"))
    {
      fullScreen(sketchDisplay());
    }
    else
    {
      parseAndSetConfigSize();
    }

    Map<String, String> params = getParameterMap();
    smooth(DefaultValueParser.parseInt(
      params.get(sketchRenderer() + ".smooth"), sketchSmooth()));
  }


  private void parseAndSetConfigSize()
  {
    OptionalInt width = getInt(preferences, PREF_GEOMETRY + "width"),
      height = getInt(preferences, PREF_GEOMETRY + "height");
    int iWidth = 0, iHeight = 0;
    if (width.isPresent() && height.isPresent())
    {
      iWidth = width.getAsInt();
      iHeight = height.getAsInt();
    }
    else
    {
      int[] aSize = parseParamIntDimensions("size");
      if (aSize != null)
      {
        iWidth = aSize[0];
        iHeight = aSize[1];
      }
    }
    if (iWidth > 0 || iHeight > 0)
      size(Math.max(iWidth, MIN_DIMENSION), Math.max(iHeight, MIN_DIMENSION));
  }


  @Override
  protected PSurface initSurface()
  {
    PSurface surface = super.initSurface();
    if (parent != null && P3D.equals(sketchRenderer()))
    {
      /*
       * Invoke parent#exit() instead of PApplet#exit() when closing the sketch
       * window.
       */
      ((Window) surface.getNative()).addWindowListener(
        0, new ParentApplicationWindowDestructionListener());
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
    parseAndSetSurfaceLocation();
    showSurfaceLatch.countDown();
    showSurfaceLatch = null;
    super.showSurface();
  }


  private void parseAndSetSurfaceLocation()
  {
    if (!P3D.equals(sketchRenderer()))
      return;

    final Window window = (Window) getSurface().getNative();
    int[] aLocation = null;
    OptionalInt left = getInt(preferences, PREF_GEOMETRY + "left"),
      top = getInt(preferences, PREF_GEOMETRY + "top");
    if (left.isPresent() && top.isPresent())
    {
      int iLeft = left.getAsInt(), iTop = top.getAsInt();
      RectangleImmutable intersection =
        window.getScreen().getViewport().intersection(
          iLeft, iTop, iLeft + this.width, iTop + this.height);
      if (Math.min(intersection.getWidth(), intersection.getHeight()) >= MIN_DIMENSION)
        aLocation = new int[]{iLeft, iTop};
    }

    if (aLocation == null)
      aLocation = parseParamIntDimensions("location");

    if (aLocation != null)
      window.setPosition(aLocation[0], aLocation[1]);
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
       * However, with the follow trick we can send a fake null exception as
       * signal to that thread which leads to voluntary its termination.
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
    savePreferences();
    super.dispose();
  }


  protected final void savePreferences()
  {
    doSavePreferences();
    PreferenceUtils.flush(preferences);
  }


  protected void doSavePreferences()
  {
    if (!sketchFullScreen())
    {
      preferences.putInt(PREF_GEOMETRY + "width", width);
      preferences.putInt(PREF_GEOMETRY + "height", height);

      if (P3D.equals(sketchRenderer()))
      {
        PointImmutable l = getSurfaceLocation(savedSurfaceLocation);
        preferences.putInt(PREF_GEOMETRY + "left", l.getX());
        preferences.putInt(PREF_GEOMETRY + "top", l.getY());
      }
    }
  }


  public Map<String, String> getParameterMap()
  {
    return parent.getNamedParameters();
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
      !System.getProperty("javafx.runtime.version", "").startsWith("8.") ?
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
    return thread(PImageFutures.from(url));
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


  public enum ImageResizeMode
  {
    STRETCH,
    PAN
  }

  public void image( PImage img, ImageResizeMode resizeMode,
    float dstLeft, float dstTop, float dstWidth, float dstHeight )
  {
    int srcLeft = 0, srcTop = 0, srcWidth = img.width, srcHeight = img.height;

    if (dstWidth <= 0 || dstHeight <= 0) {
      throw new IllegalArgumentException("Image destination has zero area");
    }
    if (srcWidth <= 0 || srcHeight <= 0) {
      throw new IllegalArgumentException("Image source has zero area");
    }
    if (g.imageMode != CORNER) {
      throw new UnsupportedOperationException(
        "Image modes besides CORNER are currently unimplemented");
    }

    switch (resizeMode) {
    case STRETCH:
      break;

    case PAN:
      float dstRatio = dstWidth / dstHeight,
        srcRatio = (float) srcWidth / srcHeight;
      if (srcRatio > dstRatio) {
        srcWidth = (int)(srcHeight * dstRatio + 0.5f);
        srcLeft = (img.width - srcWidth) / 2;
      } else if (srcRatio < dstRatio) {
        srcHeight = (int)(srcWidth / dstRatio + 0.5f);
        srcTop = (img.height - srcHeight) / 2;
      }
      break;
    }

    image(img, dstLeft, dstTop, dstWidth, dstHeight, srcLeft, srcTop, srcWidth, srcHeight);
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
        if (filename.endsWith(".bmp"))
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
        ExtPApplet.super.save(filename);
        break;
      }
    });
  }


  public <R extends Runnable> R thread( R action )
  {
    executorService.execute(action);
    return action;
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


  public Point getSurfaceLocation( Point l )
  {
    if (!checkRendererSupported("get window bounds", true))
      return null;

    NativeWindow w = (NativeWindow) getSurface().getNative();
    return w.getLocationOnScreen(l);
  }


  public Rectangle getWindowBounds( Rectangle r )
  {
    if (!checkRendererSupported("get window bounds", true))
      return null;
    NativeWindow w = (NativeWindow) getSurface().getNative();
    InsetsImmutable insets = w.getInsets();
    Point location = w.getLocationOnScreen(null);
    if (r == null)
      r = new Rectangle();
    r.set(location.getX() - insets.getLeftWidth(),
      location.getY() - insets.getTopHeight(),
      w.getWidth() + insets.getTotalWidth(),
      w.getHeight() + insets.getTotalHeight());
    return r;
  }


  private final Point savedSurfaceLocation = new Point();


  public synchronized boolean toggleFullscreen()
  {
    if (!checkRendererSupported("toggle fullscreen"))
      return sketchFullScreen();

    Window w = (Window) getSurface().getNative();
    boolean currentFullscreenState = w.isFullscreen();
    PointImmutable windowLocation;
    //noinspection IfMayBeConditional
    if (currentFullscreenState)
    {
      windowLocation = savedSurfaceLocation;
      /*
      System.out.format(
        "Restoring previous window location (%d, %d)...%n",
        windowLocation.getX(), windowLocation.getY());
        */
    }
    else
    {
      windowLocation = getSurfaceLocation(savedSurfaceLocation);
      /*
      System.out.format(
        "Stored current window location (%d, %d) for later user (screen index %d).%n",
        windowLocation.getX(), windowLocation.getY(), w.getScreenIndex());
      */
    }

    boolean newFullScreenState = WindowSupport.toggleFullscreen(w, windowLocation);
    if (newFullScreenState == currentFullscreenState)
    {
      System.err.format(
        "Couldn't set fullscreen state to %s on %s.%n",
        !currentFullscreenState, w);
    }
    return newFullScreenState;
  }


  protected final boolean checkRendererSupported( String operationDescription )
  {
    return checkRendererSupported(operationDescription, false);
  }

  protected boolean checkRendererSupported( String operationDescription,
    boolean doThrow )
  {
    String renderer = sketchRenderer();
    boolean supported = P3D.equals(renderer);

    if (!supported)
    {
      String msgFormat =
        "The following operation is currently not supported for the %s " +
          "renderer: %s";
      Object[] msgParams = { renderer, operationDescription };
      if (doThrow)
      {
        throw new UnsupportedOperationException(
          String.format(msgFormat, msgParams));
      }
      System.err.format(msgFormat, msgParams).println();
    }

    return supported;
  }


  private class ParentApplicationWindowDestructionListener
    implements WindowListener
  {
    @Override
    public void windowResized( WindowEvent e ) { }

    @Override
    public void windowMoved( WindowEvent e ) { }

    @Override
    public void windowDestroyNotify( WindowEvent ev )
    {
      parent.exit();
      ev.setConsumed(true);
    }

    @Override
    public void windowDestroyed( WindowEvent e ) { }

    @Override
    public void windowGainedFocus( WindowEvent e ) { }

    @Override
    public void windowLostFocus( WindowEvent e ) { }

    @Override
    public void windowRepaint( WindowUpdateEvent e ) { }
  }


  private int[] parseParamIntDimensions( String key )
  {
    if (key.isEmpty())
      throw new IllegalArgumentException("empty key");

    String value = getParameterMap().get(key);
    if (value != null)
    {
      int[] result = parseIntDimensions(value);
      if (result != null)
        return result;

    }
    return null;
  }


  private static int[] parseIntDimensions( String s )
  {
    return parseIntDimensions(s, ',');
  }

  private static int[] parseIntDimensions( String s, char delimiter )
  {
    if (!s.isEmpty())
    {
      int p = s.indexOf(delimiter);
      if (p != 0) try
      {
        int x = 0, y = 0;
        boolean success = false;

        if (p < 0)
        {
          x = y = Integer.parseInt(s);
          success = true;
        }
        else if (s.length() > p + 1 && s.indexOf(delimiter, p + 1) < 0)
        {
          x = Integer.parseInt(s.substring(0, p));
          y = Integer.parseInt(s.substring(p + 1));
          success = true;
        }

        if (success)
          return new int[]{ x, y };
      }
      catch (NumberFormatException ignored)
      {
        // return default
      }
    }
    return null;
  }


  public boolean isDrawingThread()
  {
    checkRendererSupported("isDrawingThread", true);
    return ((PGraphicsOpenGL) g).pgl.threadIsCurrent();
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
  getPreferenceAdapters()
  {
    return Stream.of(targetFrameRate.getAspect(
      PropertyPreferencesAdapterTag.getWritableInstance()));
  }
}
