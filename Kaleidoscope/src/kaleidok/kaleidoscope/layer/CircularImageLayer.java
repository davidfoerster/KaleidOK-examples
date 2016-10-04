package kaleidok.kaleidoscope.layer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ObservableObjectValue;
import kaleidok.javafx.beans.property.SimpleBoundedDoubleProperty;
import kaleidok.javafx.beans.property.SimpleBoundedIntegerProperty;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.DIMENSIONS;
import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.MAX_SEGMENTS;
import static kaleidok.kaleidoscope.layer.CircularTriangleStripSegmentCoordinates.MIN_SEGMENTS;


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
    innerRadius.levelOfDetail = 5;
    outerRadius.levelOfDetail = 5;
    scaleFactor.getBounds().setAmountToStepBy(0.01);

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
    this.segmentCount.levelOfDetail = 10;
    this.segmentCoords = new CircularTriangleStripSegmentCoordinates(
      (segmentMultiplier != 1) ?
        this.segmentCount.multiply(segmentMultiplier) :
        this.segmentCount);
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void init()
  {
    LayerUtils.adjustPermilleFormat(innerRadius);
    LayerUtils.adjustPermilleFormat(outerRadius);
    LayerUtils.adjustPercentFormat(scaleFactor);
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
