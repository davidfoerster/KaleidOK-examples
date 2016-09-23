package kaleidok.kaleidoscope.layer;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.logging.LoggingUtils;
import kaleidok.util.SynchronizedFormat;
import processing.core.PImage;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class ImageLayer implements Runnable
{
  protected final ExtPApplet parent;

  public final StringProperty name;

  public final IntegerProperty wireframe =
    new SimpleIntegerProperty(this, "wireframe", 0);

  private final AtomicReference<PImageFuture> nextImage =
    new AtomicReference<>();

  private CurrentImage currentImage = CurrentImage.NULL_IMAGE;

  private final SynchronizedFormat screenshotPathPattern = new SynchronizedFormat();


  protected ImageLayer( ExtPApplet parent )
  {
    this.parent = parent;
    this.name = new SimpleStringProperty(
      this, "name", getDefaultName(getClass()));
  }


  private static String getDefaultName( Class<? extends ImageLayer> clazz )
  {
    String layerSuffix = "Layer",
      className = clazz.getSimpleName();
    return className.endsWith(layerSuffix) ?
      className.substring(
        0, className.length() - layerSuffix.length()) :
      className;
  }


  public PImageFuture getNextImage()
  {
    return nextImage.get();
  }

  public void setNextImage( PImageFuture img )
  {
    nextImage.set(img);
  }


  public PImage getCurrentImage()
  {
    PImageFuture nextFuture = nextImage.getAndSet(null);
    if (nextFuture != null)
    {
      if (!nextFuture.isDone())
      {
        nextImage.compareAndSet(null, nextFuture);
      }
      else if (!nextFuture.isCancelled()) try
      {
        PImage next = nextFuture.get();
        if (next != null && next.width > 0 && next.height > 0)
          setCurrentImage(next);
      }
      catch (InterruptedException | ExecutionException ex)
      {
        logFutureImageException(nextFuture, ex);
      }
    }
    return currentImage.image;
  }


  public void setCurrentImage( PImage image )
  {
    if (image != null)
    {
      // don't save screenshots of the initial images
      PImage currentImage = this.currentImage.image;
      if (currentImage != null && currentImage != image)
        saveScreenshot();
    }
    this.currentImage = CurrentImage.newInstance(image);
  }


  private static void logFutureImageException( PImageFuture f, Throwable t )
  {
    if (t instanceof ExecutionException)
      t = t.getCause();

    LoggingUtils.logThrown(
      Logger.getLogger(f.getClass().getCanonicalName()), Level.WARNING,
      "Couldn't construct image from: {0}", t, f);

  }


  private static final class CurrentImage
  {
    public final float txFactor, tyFactor;

    public final PImage image;

    public static final CurrentImage NULL_IMAGE = new CurrentImage();


    public static CurrentImage newInstance( PImage image )
    {
      return (image != null) ? new CurrentImage(image) : NULL_IMAGE;
    }


    private CurrentImage()
    {
      txFactor = 1;
      tyFactor = 1;
      image = null;
    }


    private CurrentImage( PImage image )
    {
      assert image.width > 0 && image.height > 0 :
        image + " has width or height ≤0";
      float
        imgWidth = image.width, imgHeight = image.height,
        imgAspect = imgWidth / imgHeight;
      if (imgAspect <= 1) {
        txFactor = 0.5f;
        tyFactor = imgAspect * 0.5f;
      } else {
        txFactor = imgHeight / imgWidth * 0.5f; // = 1 / imgAspect;
        tyFactor = 0.5f;
      }

      this.image = image;
    }
  }


  private void saveScreenshot()
  {
    if (screenshotPathPattern.hasUnderlying())
    {
      String pathName;
      synchronized (screenshotPathPattern)
      {
        if (!screenshotPathPattern.hasUnderlying())
          return;
        pathName = screenshotPathPattern.format(
          new Object[]{ new Date(), parent.frameCount }, null);
      }
      parent.save(pathName, true);
    }
  }


  public void setScreenshotPathPattern( MessageFormat format )
  {
    synchronized (screenshotPathPattern)
    {
      screenshotPathPattern.setUnderlying(format);
    }
  }


  protected void drawVertex( float x, float y )
  {
    CurrentImage img = currentImage;
    parent.vertex(x, y, x * img.txFactor + 0.5f, y * img.tyFactor + 0.5f);
  }
}
