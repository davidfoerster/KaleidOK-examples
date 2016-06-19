package kaleidok.awt;

import kaleidok.concurrent.NotifyFuture;
import org.apache.http.concurrent.FutureCallback;

import java.awt.Component;
import java.awt.Image;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.awt.image.ImageObserver.*;


public abstract class ReadyImageFuture extends NotifyFuture<Image>
{
  public static final int
    SUCCESS = ALLBITS | FRAMEBITS,
    DONE = SUCCESS | ERROR | ABORT;


  protected final Image image;

  protected volatile FutureCallback<Image> callback = null;

  protected volatile int statusFlags;


  public static ReadyImageFuture createInstance( Image image )
  {
    return new ReadyImageFutureImpl(image);
  }

  public static ReadyImageFuture createInstance( Component comp, Image image )
  {
    return createInstance(comp, image, null);
  }

  public static ReadyImageFuture createInstance( Component parent, Image image,
    FutureCallback<Image> callback )
  {
    return createInstance(parent, image, -1, -1, callback);
  }

  public static ReadyImageFuture createInstance( Component comp, Image image,
    int width, int height, FutureCallback<Image> callback )
  {
    return new ReadyImageFutureImpl(comp, image, width, height, callback);
  }


  protected ReadyImageFuture( Image image )
  {
    this.image = image;
  }

  public ReadyImageFuture( Component comp, Image image, int width, int height,
    FutureCallback<Image> callback )
  {
    this(image);
    this.callback = callback;
    prepareImpl(comp, width, height);
  }


  protected abstract boolean prepareImpl( Component comp, int width, int height );

  public boolean prepare( Component comp )
  {
    return prepare(comp, -1, -1);
  }

  public boolean prepare( Component comp, int width, int height )
  {
    return (statusFlags & (SUCCESS | ERROR)) != 0 ||
      prepareImpl(comp, width, height);
  }

  @Override
  public boolean cancel( boolean mayInterruptIfRunning )
  {
    return false;
  }

  public int getStatus()
  {
    return statusFlags;
  }

  @Override
  public boolean isCancelled()
  {
    return (statusFlags & ABORT) != 0;
  }

  @Override
  public boolean isDone()
  {
    return (statusFlags & DONE) != 0;
  }

  public boolean isSuccessful()
  {
    return (statusFlags & SUCCESS) != 0;
  }


  @Override
  public Image get() throws InterruptedException
  {
    waitFor();
    return getCurrent();
  }

  @Override
  public Image get( long timeout, TimeUnit unit )
    throws InterruptedException, TimeoutException
  {
    waitFor(timeout, unit);
    return getCurrent();
  }

  private Image getCurrent()
  {
    if (isCancelled())
      throw new CancellationException();
    return isSuccessful() ? image : null;
  }


  protected void notifyFutureCallback( Image image, int infoFlags )
  {
    FutureCallback<Image> cb = this.callback;
    if (cb != null) {
      if ((infoFlags & SUCCESS) != 0) {
        cb.completed(image);
      } else if ((infoFlags & ABORT) != 0) {
        cb.cancelled();
      } else if ((infoFlags & ERROR) != 0) {
        cb.failed(null);
      }
    }
  }
}
