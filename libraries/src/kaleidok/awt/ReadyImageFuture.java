package kaleidok.awt;

import kaleidok.concurrent.NotifyFuture;

import java.awt.Component;
import java.awt.Image;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.awt.image.ImageObserver.*;


public abstract class ReadyImageFuture extends NotifyFuture<Image>
{
  protected final Image image;

  protected volatile int statusFlags;

  public static ReadyImageFuture createInstance( Image image )
  {
    return new ReadyImageFutureImpl(image);
  }

  public static ReadyImageFuture createInstance( Component comp, Image image )
  {
    return createInstance(comp, image, -1, -1);
  }

  public static ReadyImageFuture createInstance( Component comp, Image image, int width, int height )
  {
    return new ReadyImageFutureImpl(comp, image, width, height);
  }

  public ReadyImageFuture( Image image )
  {
    this.image = image;
  }

  public ReadyImageFuture( Component comp, Image image, int width, int height )
  {
    this(image);
    prepareImpl(comp, width, height);
  }

  protected abstract boolean prepareImpl( Component comp, int width, int height );

  public boolean prepare( Component comp )
  {
    return prepare(comp, -1, -1);
  }

  public boolean prepare( Component comp, int width, int height )
  {
    return (statusFlags & (ALLBITS | FRAMEBITS | ERROR)) != 0 ||
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
    return isDone(statusFlags);
  }

  public boolean isSuccessful()
  {
    return (statusFlags & (ALLBITS | FRAMEBITS)) != 0;
  }

  public static boolean isDone( int statusFlags )
  {

    return (statusFlags & (ALLBITS | FRAMEBITS | ERROR | ABORT)) != 0;
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
}
