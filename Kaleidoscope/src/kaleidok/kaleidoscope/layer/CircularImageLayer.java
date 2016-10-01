package kaleidok.kaleidoscope.layer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ObservableObjectValue;
import kaleidok.javafx.beans.property.SimpleBoundedDoubleProperty;
import kaleidok.javafx.beans.property.SimpleBoundedIntegerProperty;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;

import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.DIMENSIONS;
import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.MAX_SEGMENTS;
import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.MIN_SEGMENTS;
import static kaleidok.kaleidoscope.layer.LayerUtils.adjustPercentFormat;


public abstract class CircularImageLayer extends ImageLayer
{
  public static final int MAX_SEGMENT_MULTIPLIER = MAX_SEGMENTS / MIN_SEGMENTS;

  protected final SimpleBoundedDoubleProperty
    innerRadius = new SimpleBoundedDoubleProperty(
      this, "inner radius", 0.25, 0, 1, 0.025),
    outerRadius = new SimpleBoundedDoubleProperty(
      this, "outer radius", 0.50, 0, 1, 0.025),
    scaleFactor = SimpleBoundedDoubleProperty.forFloat(
      this, "scale factor", 1);

  public final SimpleBoundedIntegerProperty segmentCount;

  private final ObservableObjectValue<float[]> segmentCoords;


  protected CircularImageLayer( ExtPApplet parent, int segmentCount )
  {
    this(parent, segmentCount, 1);
  }


  protected CircularImageLayer( ExtPApplet parent, int segmentCount,
    int segmentMultiplier )
  {
    super(parent);

    adjustPercentFormat(innerRadius);
    adjustPercentFormat(outerRadius);
    scaleFactor.getBounds().setAmountToStepBy(0.01);
    adjustPercentFormat(scaleFactor);

    if (segmentMultiplier < 1 ||
      segmentMultiplier > MAX_SEGMENT_MULTIPLIER )
    {
      throw new IllegalArgumentException(
        "segment multiplier outside of [1, " + MAX_SEGMENT_MULTIPLIER +
          "]: " + segmentMultiplier);
    }

    this.segmentCount =
      new SimpleBoundedIntegerProperty(this, "segment count", segmentCount,
        MIN_SEGMENTS, MAX_SEGMENTS / segmentMultiplier);
    this.segmentCoords = new CircularTriangleStripSegmentCoordinates(
      (segmentMultiplier != 1) ?
        this.segmentCount.multiply(segmentMultiplier) :
        this.segmentCount);
  }


  public IntegerProperty segmentCountProperty()
  {
    return segmentCount;
  }


  public DoubleProperty innerRadiusProperty()
  {
    return innerRadius;
  }


  public DoubleProperty outerRadiusProperty()
  {
    return outerRadius;
  }


  public DoubleProperty scaleFactorProperty()
  {
    return scaleFactor;
  }


  protected void drawCircleVertex( int index, float radius )
  {
    final float[] segmentCoords = this.segmentCoords.get();
    drawVertex(
      segmentCoords[index * DIMENSIONS] * radius,
      segmentCoords[index * DIMENSIONS + 1] * radius);
  }


  protected void drawDebugCircle( float radius )
  {
    PApplet parent = this.parent;
    assert parent.g.ellipseMode == PConstants.RADIUS;
    parent.noFill();
    parent.ellipse(0, 0, radius, radius);
  }
}
