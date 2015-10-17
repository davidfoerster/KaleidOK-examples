package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import processing.core.PImage;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;


public abstract class ImageLayer implements Runnable
{
  protected final ExtPApplet parent;

  public int wireframe = 0;

  private final AtomicReference<PImageFuture> nextImage =
    new AtomicReference<>();

  private float txFactor = 1;

  private float tyFactor = 1;

  private PImage currentImage = null;

  public MessageFormat screenshotPathPattern = null;


  public ImageLayer( ExtPApplet parent )
  {
    this.parent = parent;
  }


  public PImageFuture getNextImage()
  {
    return nextImage.get();
  }

  public void setNextImage( PImageFuture img )
  {
    nextImage.set(img);
  }


  public PImage getCurrentImage()
  {
    PImageFuture nextFuture = nextImage.getAndSet(null);
    if (nextFuture != null)
    {
      PImage next = nextFuture.getNoThrow();
      if (next != null && next.width > 0 && next.height > 0)
        setCurrentImage(next);
    }
    return currentImage;
  }


  public void setCurrentImage( PImage img )
  {
    if (img != null && img != currentImage)
    {
      assert img.width > 0 && img.height > 0 :
        img + " has width or height ≤0";
      float
        imgWidth = img.width, imgHeight = img.height,
        imgAspect = imgWidth / imgHeight;
      if (imgAspect <= 1) {
        txFactor = 0.5f;
        tyFactor = imgAspect * 0.5f;
      } else {
        txFactor = imgHeight / imgWidth * 0.5f; // = 1 / imgAspect;
        tyFactor = 0.5f;
      }

      // don't save screenshots of the initial images
      if (currentImage != null && screenshotPathPattern != null) {
        StringBuffer screenshotPath =
          screenshotPathPattern.format(
            new Object[]{new Date(), parent.frameCount}, new StringBuffer(32),
            null);
        parent.save(screenshotPath.toString(), true);
      }
    }
    currentImage = img;
  }


  protected void drawVertex( float x, float y )
  {
    parent.vertex(x, y, x * txFactor + 0.5f, y * tyFactor + 0.5f);
  }
}
