package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;


public abstract class CircularLayer implements Runnable
{
  protected final PApplet parent;

  public final int segmentCount;

  public float innerRadius, outerRadius;

  public float scaleFactor = 1;

  public volatile PImageFuture currentImage;

  private final float[] xL, yL;


  public CircularLayer(PApplet parent, PImageFuture img, int segmentCount,
    float innerRadius, float outerRadius)
  {
    this.parent = parent;
    this.currentImage = img;
    this.segmentCount = segmentCount;
    this.innerRadius = innerRadius;
    this.outerRadius = outerRadius;

    xL = new float[segmentCount];
    yL = new float[segmentCount];
    initAngles();
  }


  private void initAngles()
  {
    double step = Math.PI * 2 / segmentCount; // generate the step size based on the number of segments
    // pre-calculate x and y based on angle and store values in two arrays
    for (int i = 0; i < segmentCount; i++) {
      double theta = step * i; // angle for this segment
      xL[i] = (float) Math.sin(theta);
      yL[i] = (float) Math.cos(theta);
    }
  }


  // custom method that draws a vertex with correct position and texture coordinates
  // based on index and a diameter input parameters
  protected void drawCircleVertex( int index, float radius )
  {
    float
      x = xL[index] * radius, tx = x,
      y = yL[index] * radius, ty = y;

    PImage img = currentImage.getNoThrow();
    if (img != null) {
      assert img.width > 0 && img.height > 0;
      float imgAspect = (float) img.width / img.height;
      if (imgAspect <= 1) {
        ty *= imgAspect;
      } else {
        tx /= imgAspect;
      }
    }

    // draw vertex with the calculated position and texture coordinates
    parent.vertex(x, y, (tx + 1) / 2, (ty + 1) / 2);
  }
}
