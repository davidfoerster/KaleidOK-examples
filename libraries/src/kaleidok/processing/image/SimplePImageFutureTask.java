package kaleidok.processing.image;

import processing.core.PImage;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


public final class SimplePImageFutureTask extends FutureTask<PImage> implements PImageFuture
{
  private final Object source;


  SimplePImageFutureTask( Callable<PImage> callable, Object source )
  {
    super(callable);
    this.source = source;
  }


  @Override
  public String toString()
  {
    return PImageFuture.class.getCanonicalName() + " for " + source;
  }
}
