package kaleidok.examples.kaleidoscope.layer;

import processing.core.PApplet;


public abstract class CircularImageLayer extends ImageLayer
{

  private float innerRadius, outerRadius;

  private float scaleFactor = 1;

  private float[] xL = null, yL = null;


  public CircularImageLayer( PApplet parent, int segmentCount,
    float innerRadius, float outerRadius )
  {
    super(parent);
    setSegmentCount(segmentCount);
    setInnerRadius(innerRadius);
    setOuterRadius(outerRadius);
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


  protected void drawCircleVertex( int index, float radius )
  {
    drawVertex(xL[index] * radius, yL[index] * radius);
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
