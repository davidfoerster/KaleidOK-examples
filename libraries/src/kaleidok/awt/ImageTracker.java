package kaleidok.awt;

import kaleidok.util.DebugManager;
import kaleidok.util.Threads;
import sun.awt.AppContext;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;


public class ImageTracker implements ImageObserver
{
  public static final int
    SUCCESS = ALLBITS | FRAMEBITS,
    DONE = SUCCESS | ERROR | ABORT;


  public final Toolkit toolkit;

  public final Image image;

  private volatile int infoFlags = 0;


  public ImageTracker( Image image )
  {
    this(Toolkit.getDefaultToolkit(), image);
  }

  public ImageTracker( Toolkit toolkit, Image image )
  {
    this.toolkit = toolkit;
    this.image = image;
  }


  public static Image loadImage( byte[] imageBytes )
  {
    Toolkit tk = Toolkit.getDefaultToolkit();
    ImageTracker tracker =
      new ImageTracker(tk, tk.createImage(imageBytes));
    tracker.waitForImage();
    return ((tracker.getInfoFlags() & SUCCESS) != 0) ? tracker.image : null;
  }


  public int getInfoFlags()
  {
    return infoFlags;
  }


  @Override
  public boolean imageUpdate( Image image, int infoFlags, int x, int y,
    int width, int height )
  {
    if (image == this.image) {
      this.infoFlags = infoFlags;
      if ((infoFlags & DONE) != 0) {
        synchronized (this) {
          notifyAll();
        }
        return false;
      }
    }
    return true;
  }


  public void waitForImage()
  {
    waitForStatus(0);
  }

  public void waitForStatus( int infoMask )
  {
    infoMask |= DONE;
    if ((infoFlags & infoMask) == 0) {
      synchronized (this) {
        if ((infoFlags & infoMask) == 0 &&
          !toolkit.prepareImage(image, -1, -1, this))
        {
          adjustImageFetcherPriority();
          //DebugManager.printThreads(null, null, false);

          do {
            try {
              wait();
              break;
            } catch (InterruptedException ex) {
              // go on...
            }
          } while ((infoFlags & infoMask) == 0);
        }
      }
    }
  }


  public static int IMAGE_FETCHER_PRIORITY = Thread.MIN_PRIORITY + 1;

  static void adjustImageFetcherPriority()
  {
    adjustImageFetcherPriority(IMAGE_FETCHER_PRIORITY);
  }


  private static final Object IMAGE_FETCHER_ADJUSTMENT_LOCK = new Object();

  private static void adjustImageFetcherPriority( int newPriority )
  {
    final ThreadGroup tg = AppContext.getAppContext().getThreadGroup();

    if (newPriority > tg.getMaxPriority()) {
      System.err.format(
        "Warning: The requested thread priority %d exceeds the maximum priority %d of the pertaining thread group \"%s\".%n",
        newPriority, tg.getMaxPriority(), tg.getName());
      return;
    }

    synchronized (IMAGE_FETCHER_ADJUSTMENT_LOCK)
    {
      for (final Thread t: Threads.getThreads(tg, false)) {
        if (t.getName().startsWith("Image Fetcher ")) {
          final int oldPriority = t.getPriority();
          if (newPriority != oldPriority) {
            t.setPriority(newPriority);
            if (DebugManager.verbose >= 5) {
              System.out.format(
                "Changed priority of thread \"%s\" (%d) from %d to %d.%n",
                t.getName(), t.getId(), oldPriority, newPriority);
            }
          }
        }
      }
    }
  }
}
