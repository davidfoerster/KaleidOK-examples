package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Draws a circular shape based on a triangle fan or triangle strip.
 * <p>
 * Inheriting classes need to implement the <code>{@link #run()}</code> method
 * with their custom drawing algorithm.
 */
public abstract class CircularLayer implements Runnable
{
  /**
   * The parent canvas object for this shape
   */
  protected final PApplet parent;

  private float innerRadius, outerRadius;

  private float scaleFactor = 1;

  private final AtomicReference<PImageFuture> nextImage =
    new AtomicReference<>();

  private PImage currentImage = null;

  private float[] xL = null, yL = null;


  /**
   * @param parent  A parent object
   * @param img  A texture image (may be <code>null</code>)
   * @param segmentCount  A segment count
   * @param innerRadius  An inner radius
   * @param outerRadius  An outer radius
   *
   * @see #parent
   * @see #setNextImage(PImageFuture)
   * @see #setSegmentCount(int)
   * @see #setInnerRadius(float)
   * @see #setOuterRadius(float)
   */
  public CircularLayer(PApplet parent, PImageFuture img, int segmentCount,
    float innerRadius, float outerRadius)
  {
    this.parent = parent;
    setSegmentCount(segmentCount);
    setInnerRadius(innerRadius);
    setOuterRadius(outerRadius);
    setNextImage(img);
  }


  /**
   * @return  The current segment count
   * @see #setSegmentCount(int)
   */
  public int getSegmentCount()
  {
    return xL.length;
  }

  /**
   * Sets the number of triangular segments used to draw the circular shape.
   *
   * @param segmentCount  A segment number
   */
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


  /**
   * @return  The current inner radius
   * @see #setInnerRadius(float)
   */
  public float getInnerRadius()
  {
    return innerRadius;
  }

  /**
   * Sets the inner radius of a ring-like shape. For circles and discs this
   * property meaningless.
   *
   * @param innerRadius  A radius
   */
  public void setInnerRadius( float innerRadius )
  {
    this.innerRadius = innerRadius;
  }


  /**
   * @return  The current outer radius
   * @see #setOuterRadius(float)
   */
  public float getOuterRadius()
  {
    return outerRadius;
  }


  /**
   * Sets the outer radius of a circular shape.
   *
   * @param outerRadius  A radius
   */
  public void setOuterRadius( float outerRadius )
  {
    this.outerRadius = outerRadius;
  }


  /**
   * @return  The current scale factor
   * @see #setScaleFactor(float)
   */
  public float getScaleFactor()
  {
    return scaleFactor;
  }

  /**
   * Sets the “scale factor” of this shape. The meaning depends on the
   * implementing class.
   *
   * @param scaleFactor  A factor (usually between 0 and 1)
   */
  public void setScaleFactor( float scaleFactor )
  {
    this.scaleFactor = scaleFactor;
  }


  /**
   * @return  A future reference to the next image
   * @see #setNextImage(PImageFuture)
   */
  public PImageFuture getNextImage()
  {
    return nextImage.get();
  }

  /**
   * Sets the next image (in the form of a future reference) to use as the
   * texture for this shape.
   *
   * @param img  A future reference to an image
   */
  public void setNextImage( PImageFuture img )
  {
    nextImage.set(img);
  }


  /**
   * Returns the currently used texture image. If the next image became
   * available, it first updates the currently used image.
   *
   * @return  The current texture image
   * @see #setNextImage(PImageFuture)
   */
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

  /**
   * Sets the image to use as a texture for this shape. Non-quadratic images
   * are panned to fit into this shape.
   *
   * @param img  An image
   */
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
        txFactor = 1;
        tyFactor = imgAspect;
      } else {
        txFactor = imgHeight / imgWidth; // = 1 / imgAspect;
        tyFactor = 1;
      }
    }
    currentImage = img;
  }

  private float txFactor = 1, tyFactor = 1;


  /**
   * Draws a single vertex of this circular shape. The texture coordinates are
   * chosen accordingly.
   *
   * @param index  The circle segment belonging to the vertex (in clockwise order)
   * @param radius  The distance of the vertex from the centre of this shape
   */
  protected void drawCircleVertex( int index, float radius )
  {
    float
      x = xL[index] * radius, tx = x * txFactor,
      y = yL[index] * radius, ty = y * tyFactor;

    // draw vertex with the calculated position and texture coordinates
    parent.vertex(x, y, (tx + 1) / 2, (ty + 1) / 2);
  }


  protected void drawDebugCircle( float radius )
  {
    PApplet parent = this.parent;
    assert parent.g.ellipseMode == PApplet.RADIUS;
    parent.noFill();
    parent.strokeWeight(1);
    parent.ellipse(0, 0, radius, radius);
  }
}
