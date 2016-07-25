package kaleidok.processing.image;

import kaleidok.util.concurrent.ImmediateFuture;
import processing.core.PImage;


public final class ImmediatePImageFuture
  extends ImmediateFuture<PImage> implements PImageFuture
{
  public ImmediatePImageFuture( PImage value )
  {
    super(value);
  }


  @Override
  public String toString()
  {
    return "Future wrapper of " + get();
  }
}
