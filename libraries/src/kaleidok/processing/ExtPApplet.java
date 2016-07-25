package kaleidok.processing;

import javafx.application.HostServices;
import kaleidok.processing.export.ImageSaveSet;
import kaleidok.processing.image.ImageIO;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.Threads;
import kaleidok.util.concurrent.GroupedThreadFactory;
import processing.core.PApplet;
import processing.core.PImage;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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


  public ExtPApplet( ProcessingSketchApplication<? extends ExtPApplet> parent )
  {
    this.parent = parent;
  }


  @OverridingMethodsMustInvokeSuper
  public void settings()
  {
    executorService = new ThreadPoolExecutor(
      0, 16, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new GroupedThreadFactory(
        getClass().getSimpleName() + " worker pool", true));
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void setup()
  {
    try
    {
      /*
       * Older Processing releases don't have a "settings" method that they
       * call on initiation, so we do it for them.
       *
       * TODO: Remove this check once the transition to such a version is
       * complete.
       */
      PApplet.class.getMethod("settings", EMPTY_CLASS_ARRAY);
    }
    catch (NoSuchMethodException ignored)
    {
      settings();
    }
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

  @Override
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
}
