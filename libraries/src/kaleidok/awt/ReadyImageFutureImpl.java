package kaleidok.awt;

import org.apache.http.concurrent.FutureCallback;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.ImageObserver;


class ReadyImageFutureImpl extends ReadyImageFuture implements ImageObserver
{
  public ReadyImageFutureImpl( Image image )
  {
    super(image);
  }

  public ReadyImageFutureImpl( Component comp, Image image, int width, int height,
    FutureCallback<Image> callback )
  {
    super(image);
    this.callback = callback;
    prepare(comp, width, height);
  }


  @Override
  protected boolean prepareImpl( Component comp, int width, int height )
  {
    boolean done = comp.prepareImage(image, width, height, this);
    if (!done) {
      ImageTracker.adjustImageFetcherPriority();
      //DebugManager.printThreads(null, null, false);
    }
    statusFlags = done ? SUCCESS | WIDTH | HEIGHT | PROPERTIES : 0;
    notifyFutureCallback(image, statusFlags);
    return done;
  }


  @SuppressWarnings("NakedNotify")
  @Override
  public boolean imageUpdate( Image img, int infoFlags, int x, int y, int width, int height )
  {
    if (img != this.image)
      return true;

    // Assume that flags are never deleted during subsequent updates.
    //assert (statusFlags & ~infoFlags) == 0;

    notifyFutureCallback(img, infoFlags);

    if (isDone()) {
      statusFlags = infoFlags;
      return false;
    }

    statusFlags = infoFlags;
    if ((infoFlags & DONE) != 0) {
      synchronized (this) {
        notifyAll();
      }
      return false;
    }
    return true;
  }
}
