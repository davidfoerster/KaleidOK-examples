package kaleidok.processing;

import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
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
import processing.core.PApplet;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private ProcessingSketchApplication<? extends ExtPApplet> parent;

  public final Set<String> saveFilenames = new ImageSaveSet(this);

  protected ExecutorService executorService;

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
    Map<String, String> params = getParameterMap();

    String sSize = params.get("size");
    if (sSize != null)
    {
      String[] asSize = split(sSize, ',');
      int x = parseInt(asSize[0]);
      size(x, (asSize.length >= 2) ? parseInt(asSize[1]) : x,
        sketchRenderer());
    }

    smooth(DefaultValueParser.parseInt(
      params.get(sketchRenderer() + ".smooth"), sketchSmooth()));

    if (parent.getUnnamedBooleanParameter("fullscreen"))
      this.fullScreen();
  }


  @Override
  protected PSurface initSurface()
  {
    PSurface surface = super.initSurface();
    Map<String, String> params = getParameterMap();

    String sResizable = params.get("resizable");
    if (sResizable != null)
      surface.setResizable(DefaultValueParser.parseBoolean(sResizable));

    return surface;
  }


  @Override
  public void setup()
  {
    Map<String, String> params = getParameterMap();
    String sFrameRate = params.get("framerate");
    if (sFrameRate != null)
      frameRate(Float.parseFloat(sFrameRate));
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
  public void save( String filename )
  {
    loadPixels();
    thread(() ->
    {
      if ((g.format == RGB || g.format == ARGB) && filename.endsWith(".bmp"))
      {
        Path filePath = Paths.get(savePath(filename), EMPTY_STRING_ARRAY);
        try {
          ImageIO.saveBmp32(filePath, width, height, pixels, 0).force();
        } catch (UnsupportedOperationException ignored) {
          // try again with default code path
        }
        catch (IOException ex)
        {
          Threads.handleUncaught(ex);
        }
      }

      ExtPApplet.super.save(filename);
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
      throw new AssertionError(ex);
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


  private final Point windowLocation = new Point();

  private PointImmutable saveWindowLocation()
  {
    Point l = windowLocation;
    NativeWindow w = (NativeWindow) getSurface().getNative();
    w.getLocationOnScreen(l);
    InsetsImmutable insets = w.getInsets();
    l.set(l.getX() - insets.getLeftWidth(), l.getY() - insets.getTopHeight());
    return l;
  }


  public boolean toggleFullscreen()
  {
    if (!P3D.equals(sketchRenderer()))
    {
      System.err.format(
        "Toggling the fullscreen state is currently not supported for the " +
          "%s renderer.%n",
        sketchRenderer());
      return sketchFullScreen();
    }

    Window w = (Window) getSurface().getNative();
    boolean currentFullscreenState = w.isFullscreen();
    PointImmutable windowLocation;
    //noinspection IfMayBeConditional
    if (currentFullscreenState)
    {
      windowLocation = this.windowLocation;
      /*System.out.format(
        "Restoring previous window location (%d, %d)...%n",
        windowLocation.getX(), windowLocation.getY());*/
    }
    else
    {
      windowLocation = saveWindowLocation();
      /*System.out.format(
        "Stored current window location (%d, %d) for later user (screen index %d).%n",
        windowLocation.getX(), windowLocation.getY(), w.getScreenIndex());*/
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
}
