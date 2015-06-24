package kaleidok.awt;

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
}
