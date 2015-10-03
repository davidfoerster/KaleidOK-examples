package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;


public abstract class CircularLayer implements Runnable
{
  protected final PApplet parent;

  private float innerRadius, outerRadius;

  private float scaleFactor = 1;

  private PImageFuture nextImage;

  private PImage currentImage = null;

  private float[] xL = null, yL = null;


  public CircularLayer(PApplet parent, PImageFuture img, int segmentCount,
    float innerRadius, float outerRadius)
  {
    this.parent = parent;
    setSegmentCount(segmentCount);
    setInnerRadius(innerRadius);
    setOuterRadius(outerRadius);
    setNextImage(img);
  }


  public int getSegmentCount()
  {
    return xL.length;
  }

  public void setSegmentCount( int segmentCount )
  {
    if (xL == null || xL.length != segmentCount) {
      xL = new float[segmentCount];
      yL = new float[segmentCount];
    }

    double step = Math.PI * 2 / segmentCount; // generate the step size based on the number of segments
    // pre-calculate x and y based on angle and store values in two arrays
    for (int i = segmentCount - 1; i >= 0; i--) {
      double theta = step * i; // angle for this segment
      xL[i] = (float) Math.sin(theta);
      yL[i] = (float) Math.cos(theta);
    }
  }


  public float getInnerRadius()
  {
    return innerRadius;
  }

  public void setInnerRadius( float innerRadius )
  {
    this.innerRadius = innerRadius;
  }


  public float getOuterRadius()
  {
    return outerRadius;
  }


  public void setOuterRadius( float outerRadius )
  {
    this.outerRadius = outerRadius;
  }


  public float getScaleFactor()
  {
    return scaleFactor;
  }

  public void setScaleFactor( float scaleFactor )
  {
    this.scaleFactor = scaleFactor;
  }


  public PImageFuture getNextImage()
  {
    return nextImage;
  }

  public void setNextImage( PImageFuture img )
  {
    this.nextImage = img;
  }


  public PImage getCurrentImage()
  {
    PImageFuture nextFuture = getNextImage();
    if (nextFuture != null) {
      PImage next = nextFuture.getNoThrow();
      if (next != null && next.width > 0 && next.height > 0)
        setCurrentImage(next);
    }
    return currentImage;
  }

  public void setCurrentImage( PImage img )
  {
    if (img != currentImage) {
      assert img.width > 0 && img.height > 0;
      float
        imgWidth = img.width, imgHeight = img.height,
        imgAspect = imgWidth / imgHeight;
      if (imgAspect <= 1) {
        txFactor = 1;
        tyFactor = imgAspect;
      } else {
        txFactor = imgHeight / imgWidth; // = 1 / imgAspect;
        tyFactor = 1;
      }
      currentImage = img;
    }
  }

  private float txFactor = 1, tyFactor = 1;


  // custom method that draws a vertex with correct position and texture coordinates
  // based on index and a diameter input parameters
  protected void drawCircleVertex( int index, float radius )
  {
    float
      x = xL[index] * radius, tx = x * txFactor,
      y = yL[index] * radius, ty = y * tyFactor;

    // draw vertex with the calculated position and texture coordinates
    parent.vertex(x, y, (tx + 1) / 2, (ty + 1) / 2);
  }
}
