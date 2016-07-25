package kaleidok.kaleidoscope.layer;

import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;


public abstract class CircularImageLayer extends ImageLayer
{

  private float innerRadius, outerRadius;

  private float scaleFactor = 1;

  private float[] segmentCoords = null;


  protected CircularImageLayer( ExtPApplet parent )
  {
    super(parent);
  }


  protected void init( int segmentCount, float innerRadius, float outerRadius )
  {
    setSegmentCount(segmentCount);
    setInnerRadius(innerRadius);
    setOuterRadius(outerRadius);
  }


  public int getSegmentCount()
  {
    return segmentCoords.length / 2;
  }

  public void setSegmentCount( int segmentCount )
  {
    if (segmentCoords == null || segmentCoords.length != segmentCount * 2)
      segmentCoords = new float[segmentCount * 2];

    final float[] segmentCoords = this.segmentCoords;
    double step = Math.PI * 2 / segmentCount; // generate the step size based on the number of segments
    // pre-calculate x and y based on angle and store values in two arrays
    for (int i = segmentCount - 1; i >= 0; i--) {
      double theta = step * i; // angle for this segment
      segmentCoords[i * 2] = (float) Math.sin(theta);
      segmentCoords[i * 2 + 1] = (float) Math.cos(theta);
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
    drawVertex(
      segmentCoords[index * 2] * radius,
      segmentCoords[index * 2 + 1] * radius);
  }


  protected void drawDebugCircle( float radius )
  {
    PApplet parent = this.parent;
    assert parent.g.ellipseMode == PConstants.RADIUS;
    parent.noFill();
    parent.ellipse(0, 0, radius, radius);
  }
}
