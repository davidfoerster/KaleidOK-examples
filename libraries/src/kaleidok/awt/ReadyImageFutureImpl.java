package kaleidok.awt;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.ImageObserver;


class ReadyImageFutureImpl extends ReadyImageFuture implements ImageObserver
{
  public ReadyImageFutureImpl( Image image )
  {
    super(image);
  }

  public ReadyImageFutureImpl( Component comp, Image image, int width, int height )
  {
    super(comp, image, width, height);
  }

  protected boolean prepareImpl( Component comp, int width, int height )
  {
    boolean done = comp.prepareImage(image, width, height, this);
    statusFlags = done ? ALLBITS | FRAMEBITS | SOMEBITS | WIDTH | HEIGHT | PROPERTIES : 0;
    return done;
  }

  @Override
  public boolean imageUpdate( Image img, int infoflags, int x, int y, int width, int height )
  {
    // Assume that flags are never deleted during subsequent updates.
    //assert (statusFlags & ~infoflags) == 0;

    if (isDone()) {
      statusFlags = infoflags;
      return false;
    }

    statusFlags = infoflags;
    if (isDone(infoflags)) {
      synchronized (this) {
        notifyAll();
      }
      return false;
    }
    return true;
  }
}
