package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;


public abstract class CircularLayer implements Runnable
{
  protected final PApplet parent;

  public final int segmentCount;

  public float innerRadius, outerRadius;

  public volatile PImageFuture currentImage;

  private final float[] xL, yL;

  public CircularLayer(PApplet parent, PImageFuture img, int segmentCount, int innerRadius, int outerRadius)
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
  protected void drawCircleVertex(int index, float diam)
  {
    float x = xL[index] * diam; // pre-calculated x direction times diameter
    float y = yL[index] * diam; // pre-calculated y direction times diameter

    PImage img = currentImage.getNoThrow();
    if (img == null) {
      parent.vertex(x, y);
    } else {
      // calculate texture coordinates based on the xy position
      float tx = x / img.width + 0.5f;
      float ty = y / img.height + 0.5f;
      // draw vertex with the calculated position and texture coordinates
      parent.vertex(x, y, tx, ty);
    }
  }

  protected void drawCircleSegment(int index)
  {
    drawCircleVertex(index, outerRadius);
  }
}
