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
import javafx.application.HostServices;
import kaleidok.newt.WindowSupport;
import kaleidok.processing.event.KeyEventSupport;
import kaleidok.processing.event.KeyStroke;
import kaleidok.processing.export.ImageSaveSet;
import kaleidok.processing.image.ImageIO;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.Arrays;
import kaleidok.util.DefaultValueParser;
import kaleidok.util.Threads;
import kaleidok.util.concurrent.GroupedThreadFactory;
import kaleidok.util.prefs.PreferenceUtils;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PSurface;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static kaleidok.util.Math.constrainInt;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;


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

  protected final List<Map<KeyStroke, Consumer<? super KeyEvent>>> keyEventHandlers;

  {
    Map<KeyStroke, Consumer<? super KeyEvent>>
      keyPressedHandlers = new HashMap<>(),
      keyReleasedHandlers = new HashMap<>(),
      keyTypedHandlers = new HashMap<>();

    keyTypedHandlers.put(
      KeyStroke.fullscreenKeystroke, ( ev ) -> thread(this::toggleFullscreen));

    keyEventHandlers = Arrays.asImmutableList(
      keyPressedHandlers, keyReleasedHandlers, keyTypedHandlers);
  }


  protected final String PREF_GEOMETRY =
    getClass().getSimpleName() + ".geometry.";


  public ExtPApplet( ProcessingSketchApplication<? extends ExtPApplet> parent )
  {
    this.parent = parent;
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void settings()
  {
    parseAndSetConfig();
    executorService = new ThreadPoolExecutor(
      0, 16, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new GroupedThreadFactory(
        getClass().getSimpleName() + " worker pool", true));
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
    int width = preferences.getInt(PREF_GEOMETRY + "width", 0),
      height = preferences.getInt(PREF_GEOMETRY + "height", 0);
    if (width <= 0 && height <= 0)
    {
      String sSize = getParameterMap().get("size");
      if (sSize != null)
      {
        String[] asSize = split(sSize, ',');
        width = parseInt(asSize[0]);
        height = (asSize.length >= 2) ? parseInt(asSize[1]) : width;
      }
    }
    if (width > 0 || height > 0)
      size(Math.max(width, MIN_DIMENSION), Math.max(height, MIN_DIMENSION));
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

    int iLeft, iTop;
    final Window window = (Window) getSurface().getNative();
    String sLocation = getParameterMap().get("location");
    if (sLocation != null)
    {
      String[] asLocation = split(sLocation, ',');
      iLeft = parseInt(asLocation[0]);
      iTop = (asLocation.length >= 2) ? parseInt(asLocation[1]) : 0;
    }
    else
    {
      double dLeft = preferences.getDouble(PREF_GEOMETRY + "left", Double.NaN),
        dTop = preferences.getDouble(PREF_GEOMETRY + "top", Double.NaN);
      if (!Double.isFinite(dLeft) || !Double.isFinite(dTop))
        return;
      iLeft = constrainInt(dLeft);
      //noinspection SuspiciousNameCombination
      iTop = constrainInt(dTop);
      RectangleImmutable intersection =
        window.getScreen().getViewport().intersection(
          iLeft, iTop, iLeft + this.width, iTop + this.height);
      if (intersection.getWidth() < MIN_DIMENSION ||
        intersection.getHeight() < MIN_DIMENSION)
      {
        return;
      }
    }
    window.setPosition(iLeft, iTop);
  }


  @Override
  public void setup()
  {
    Map<String, String> params = getParameterMap();
    String sFrameRate = params.get("framerate");
    if (sFrameRate != null)
      frameRate(Float.parseFloat(sFrameRate));
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
      Preferences preferences = this.preferences;
      preferences.putInt(PREF_GEOMETRY + "width", width);
      preferences.putInt(PREF_GEOMETRY + "height", height);

      if (P3D.equals(sketchRenderer()))
      {
        PointImmutable l = getSurfaceLocation(savedSurfaceLocation);
        preferences.putDouble(PREF_GEOMETRY + "left", l.getX());
        preferences.putDouble(PREF_GEOMETRY + "top", l.getY());
      }
    }
  }


  public Map<String, String> getParameterMap()
  {
    return parent.getNamedParameters();
  }


  private URL documentBase = null;

  public URL getDocumentBase()
  {
    if (documentBase == null) try
    {
      HostServices services = parent.getHostServices();
      if (!services.getCodeBase().isEmpty())
      {
        documentBase = new URL(services.getDocumentBase());
      }
      else
      {
        URL codeBase =
          this.getClass().getProtectionDomain().getCodeSource().getLocation();
        documentBase = new URL(codeBase, codeBase.getPath() + "data/");
      }
    }
    catch (MalformedURLException ex)
    {
      throw new InternalError(ex);
    }
    return documentBase;
  }


  public PImageFuture getImageFuture( String path )
  {
    URL url = this.getClass().getResource(path);
    if (url == null) {
      try {
        url = new URL(getDocumentBase(), path);
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(ex);
      }
      if ("file".equals(url.getProtocol())) {
        File file;
        try {
          file = new File(url.toURI());
        } catch (URISyntaxException ex) {
          throw new AssertionError(ex);
        }
        return thread(PImageFuture.from(file));
      }
    }
    return getImageFuture(url);
  }


  public PImageFuture getImageFuture( URL url )
  {
    return thread(PImageFuture.from(url));
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
      double dstRatio = (double) dstWidth / dstHeight,
        srcRatio = (double) srcWidth / srcHeight;
      if (srcRatio > dstRatio) {
        srcWidth = (int)(srcHeight * dstRatio + 0.5);
        assert srcWidth <= img.width : srcWidth + " > " + img.width;
        srcLeft = (img.width - srcWidth) / 2;
      } else if (srcRatio < dstRatio) {
        srcHeight = (int)(srcWidth / dstRatio + 0.5);
        assert srcHeight <= img.height : srcHeight + " > " + img.height;
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
        method.invoke(ExtPApplet.this, EMPTY_OBJECT_ARRAY);
      }
      catch (IllegalAccessException | IllegalArgumentException ex)
      {
        throw new AssertionError(ex);
      }
      catch (InvocationTargetException ex)
      {
        Threads.handleUncaught(ex);
      }
    });
  }


  @Override
  protected void handleKeyEvent( processing.event.KeyEvent event )
  {
    Map<KeyStroke, Consumer<? super KeyEvent>> handlers =
      keyEventHandlers.get(event.getAction() - 1);
    if (!handlers.isEmpty())
    {
      KeyEvent newtEvent = KeyEventSupport.convert(event);
      if (newtEvent != null)
      {
        for (Map.Entry<KeyStroke, Consumer<? super KeyEvent>> e :
          handlers.entrySet())
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


  public boolean toggleFullscreen()
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
          "renderer: %s.%n";
      Object[] msgParams = { renderer, operationDescription };
      if (doThrow)
      {
        throw new UnsupportedOperationException(
          String.format(msgFormat, msgParams));
      }
      System.err.format(msgFormat, msgParams);
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
}
