package kaleidok.processing;

import kaleidok.awt.ReadyImageFuture;
import processing.core.PImage;

import java.awt.Component;
import java.awt.Image;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PImageFuture implements Future<PImage>
{
  private final Future<Image> underlying;

  private PImage image = null;

  public PImageFuture( Future<Image> underlying )
  {
    this.underlying = underlying;
  }

  public PImageFuture( Component comp, Image image )
  {
    this(ReadyImageFuture.createInstance(comp, image));
  }

  public PImageFuture( Component comp, Image image, int width, int height )
  {
    this(ReadyImageFuture.createInstance(comp, image, width, height));
  }

  @Override
  public boolean cancel( boolean mayInterruptIfRunning )
  {
    return underlying.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled()
  {
    return underlying.isCancelled();
  }

  @Override
  public boolean isDone()
  {
    return underlying.isDone();
  }

  @Override
  public PImage get() throws InterruptedException, ExecutionException
  {
    return getPImage(underlying.get());
  }

  @Override
  public PImage get( long timeout, TimeUnit unit )
    throws InterruptedException, ExecutionException, TimeoutException
  {
    return getPImage(underlying.get(timeout, unit));
  }

  private PImage getPImage( Image image )
  {
    if (image == null)
      return null;
    if (this.image == null)
      this.image = new PImage(image);
    return this.image;
  }

  public PImage getNoThrow()
  {
    if (image != null)
      return image;
    if (isDone()) {
      try {
        return get();
      } catch (InterruptedException | ExecutionException ignored) {
      }
    }
    return null;
  }
}
