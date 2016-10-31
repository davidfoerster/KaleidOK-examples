package kaleidok.kaleidoscope.layer;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.logging.LoggingUtils;
import kaleidok.util.SynchronizedFormat;
import processing.core.PImage;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public abstract class ImageLayer implements Runnable, PreferenceBean
{
  protected final ExtPApplet parent;

  protected final StringProperty name;

  protected final AspectedIntegerProperty wireframe;

  private final AtomicReference<PImageFuture> nextImage =
    new AtomicReference<>();

  private CurrentImage currentImage = CurrentImage.NULL_IMAGE;

  private final SynchronizedFormat screenshotPathPattern = new SynchronizedFormat();


  protected ImageLayer( ExtPApplet parent )
  {
    this.parent = parent;
    this.name = new SimpleStringProperty(
      this, "name", getDefaultName(getClass()));

    wireframe = new AspectedIntegerProperty(this, "wireframe", 0);
    wireframe.addAspect(BoundedIntegerTag.INSTANCE,
      new IntegerSpinnerValueFactory(0, 5));
    wireframe.addAspect(LevelOfDetailTag.getInstance()).set(100);
    wireframe.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  public void init() { }


  private static String getDefaultName( Class<? extends ImageLayer> clazz )
  {
    String layerSuffix = "Layer",
      className = clazz.getSimpleName();
    return className.endsWith(layerSuffix) ?
      className.substring(
        0, className.length() - layerSuffix.length()) :
      className;
  }


  @Override
  public String getName()
  {
    return name.get();
  }


  @Override
  public Object getParent()
  {
    return parent;
  }


  public IntegerProperty wireframeProperty()
  {
    return wireframe;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return PropertyUtils.getProperties(this)
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny)
      .filter(Objects::nonNull);
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
    PImageFuture nextFuture = getNextAvailableImage();
    if (nextFuture != null && !nextFuture.isCancelled()) try
    {
      PImage next = nextFuture.get();
      if (next != null && next.width > 0 && next.height > 0)
        setCurrentImage(next);
    }
    catch (InterruptedException ex)
    {
      logFutureImageException(nextFuture, ex);
    }
    catch (ExecutionException ex)
    {
      logFutureImageException(nextFuture, ex.getCause());
    }
    return currentImage.image;
  }


  private PImageFuture getNextAvailableImage()
  {
    PImageFuture nextFuture;
    do
    {
      nextFuture = nextImage.get();
      if (nextFuture == null || !nextFuture.isDone())
        return null;
    }
    while (!nextImage.compareAndSet(nextFuture, null));

    return nextFuture;
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
