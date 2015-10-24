package kaleidok.awt;

import kaleidok.util.Threads;
import sun.awt.AppContext;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ImageTracker implements ImageObserver
{
  private static final Logger logger =
    Logger.getLogger(ImageTracker.class.getCanonicalName());

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
          do {
            try {
              wait();
              break;
            } catch (InterruptedException ex) {
              logger.log(Level.FINEST,
                "Waiting for image status was interrupted", ex);
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
      logger.log(Level.WARNING,
        "The requested thread priority {0} exceeds the maximum priority {1} " +
          "of the pertaining thread group \"{2}\"",
        new Object[]{newPriority, tg.getMaxPriority(), tg.getName()});
      return;
    }

    synchronized (IMAGE_FETCHER_ADJUSTMENT_LOCK)
    {
      for (final Thread t: Threads.getThreads(tg, false)) {
        if (t.getName().startsWith("Image Fetcher ")) {
          final int oldPriority = t.getPriority();
          if (newPriority != oldPriority) {
            t.setPriority(newPriority);
            if (logger.isLoggable(Level.FINEST)) {
              logger.log(Level.FINEST,
                "Changed priority of thread \"{0}\" ({1}) from {2} to {3}",
                new Object[]{t.getName(), t.getId(), oldPriority, newPriority});
            }
          }
        }
      }
    }
  }
}
