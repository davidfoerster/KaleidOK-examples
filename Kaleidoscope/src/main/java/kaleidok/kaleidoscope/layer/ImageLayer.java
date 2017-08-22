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
import kaleidok.util.Reflection;
import kaleidok.util.concurrent.ImmediateFuture;
import kaleidok.util.function.ChangeListener;
import kaleidok.util.logging.LoggingUtils;
import processing.core.PImage;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public abstract class ImageLayer implements Runnable, PreferenceBean
{
  protected final ExtPApplet parent;

  protected final StringProperty name;

  protected final AspectedIntegerProperty wireframe;

  private final AtomicReference<Future<PImage>> nextImage =
    new AtomicReference<>();

  private CurrentImage currentImage = CurrentImage.NULL_IMAGE;

  public ChangeListener<? super ImageLayer, ? super PImage> imageChangeCallback;


  protected ImageLayer( ExtPApplet parent )
  {
    this.parent = parent;
    this.name = new SimpleStringProperty(
      this, "name", getDefaultName(getClass()));

    wireframe = new AspectedIntegerProperty(this, "wireframe", 0);
    wireframe.addAspect(BoundedIntegerTag.getIntegerInstance(),
      new IntegerSpinnerValueFactory(0, 5));
    wireframe.addAspect(LevelOfDetailTag.getInstance()).set(100);
    wireframe.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  public void init() { }


  private static String getDefaultName( Class<? extends ImageLayer> clazz )
  {
    String layerSuffix = "Layer",
      className = Reflection.getAnonymousClassSimpleName(clazz);
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


  public Future<PImage> getNextImage()
  {
    return nextImage.get();
  }

  public void setNextImage( Future<PImage> img )
  {
    nextImage.set(img);
  }

  public void setNextImage( PImage img )
  {
    setNextImage(ImmediateFuture.of(img));
  }


  /**
   * Updates and retrieves the current image for this layer. If the
   * {@link Future} returned by {@link #getNextImage()} is
   * {@link Future#isDone() done} and, its wrapped value is used as the
   * new current image and the next image is reset as if calling
   * {@link #setNextImage(Future) setNextImage(null)}.
   * <p>
   * {@link Future#isCancelled() Cancelled} futures are skipped silently.
   * <p>
   * This method is supposed to be called from the renderer thread only.
   *
   * @return The current image for this layer.
   */
  protected PImage updateAndGetCurrentImage()
  {
    Optional<PImage> oNext = getNextAvailableImage();
    if (oNext != null)
    {
      PImage next = oNext.orElse(null),
        previous = currentImage.image;
      if (next != null && (next.width <= 0 || next.height <= 0))
        next = null;
      if (next != previous)
      {
        assert parent.isDrawingThread();
        this.currentImage = CurrentImage.newInstance(next);

        ChangeListener<? super ImageLayer, ? super PImage> callback =
          this.imageChangeCallback;
        if (callback != null)
          callback.notifyChange(this, previous, next);
      }
    }
    return currentImage.image;
  }


  @SuppressWarnings("OptionalAssignedToNull")
  private Optional<PImage> getNextAvailableImage()
  {
    Future<PImage> nextFuture;
    do
    {
      nextFuture = nextImage.get();
      if (nextFuture == null || !nextFuture.isDone())
        return null;
    }
    while (!nextImage.compareAndSet(nextFuture, null));

    if (!nextFuture.isCancelled())
    {
      try
      {
        return Optional.ofNullable(nextFuture.get());
      }
      catch (InterruptedException ex)
      {
        logFutureImageException(nextFuture, ex);
      }
      catch (ExecutionException ex)
      {
        logFutureImageException(nextFuture, ex.getCause());
      }
    }
    return null;
  }


  private void logFutureImageException( Future<PImage> f, Throwable t )
  {
    LoggingUtils.logThrown(
      Logger.getLogger(this.getClass().getCanonicalName()), Level.WARNING,
      "Couldn’t construct image from: {0}", t, f);
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
      if (image.width <= image.height)
      {
        txFactor = 0.5f;
        tyFactor = (float) image.width / image.height * 0.5f;
      }
      else
      {
        txFactor = (float) image.height / image.width * 0.5f;
        tyFactor = 0.5f;
      }

      this.image = image;
    }
  }


  protected void drawVertex( float x, float y )
  {
    CurrentImage img = currentImage;
    parent.vertex(x, y, x * img.txFactor + 0.5f, y * img.tyFactor + 0.5f);
  }
}
