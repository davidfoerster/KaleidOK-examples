package kaleidok.kaleidoscope.layer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.kaleidoscope.layer.util.CircularTriangleStripSegmentCoordinatesBinding;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import static kaleidok.kaleidoscope.layer.util.CircularTriangleStripSegmentCoordinatesBinding.DIMENSIONS;
import static kaleidok.kaleidoscope.layer.util.CircularTriangleStripSegmentCoordinatesBinding.MAX_SEGMENTS;
import static kaleidok.kaleidoscope.layer.util.CircularTriangleStripSegmentCoordinatesBinding.MIN_SEGMENTS;
import static kaleidok.kaleidoscope.layer.util.LayerUtils.adjustPercentFormat;
import static kaleidok.kaleidoscope.layer.util.LayerUtils.adjustPermilleFormat;


public abstract class CircularImageLayer extends ImageLayer
{
  public static final int MAX_SEGMENT_MULTIPLIER = MAX_SEGMENTS / MIN_SEGMENTS;

  protected final AspectedDoubleProperty innerRadius;

  protected final AspectedDoubleProperty outerRadius;

  protected final AspectedDoubleProperty scaleFactor;

  protected final AspectedIntegerProperty segmentCount;

  private final ObservableObjectValue<float[]> segmentCoords;


  protected CircularImageLayer( ExtPApplet parent, int segmentCount )
  {
    this(parent, segmentCount, 1);
  }


  protected CircularImageLayer( ExtPApplet parent, int segmentCount,
    int segmentMultiplier )
  {
    super(parent);

    if (segmentMultiplier < 1 ||
      segmentMultiplier > MAX_SEGMENT_MULTIPLIER )
    {
      throw new IllegalArgumentException(
        "segment multiplier outside of [1, " + MAX_SEGMENT_MULTIPLIER +
          "]: " + segmentMultiplier);
    }

    innerRadius = new AspectedDoubleProperty(this, "inner radius", 0.25);
    innerRadius.addAspect(LevelOfDetailTag.getInstance()).set(5);
    innerRadius
      .addAspect(BoundedDoubleTag.getDoubleInstance(), new DoubleSpinnerValueFactory(0, 1))
      .setAmountToStepBy(0.025);
    innerRadius.addAspect(PropertyPreferencesAdapterTag.getInstance());

    outerRadius = new AspectedDoubleProperty(this, "outer radius", 0.50);
    outerRadius.addAspect(LevelOfDetailTag.getInstance()).set(5);
    outerRadius
      .addAspect(BoundedDoubleTag.getDoubleInstance(), new DoubleSpinnerValueFactory(0, 1))
      .setAmountToStepBy(0.025);
    outerRadius.addAspect(PropertyPreferencesAdapterTag.getInstance());

    scaleFactor = new AspectedDoubleProperty(this, "scale factor", 1);
    scaleFactor
      .addAspect(BoundedDoubleTag.getDoubleInstance(), BoundedDoubleTag.floatBounds())
      .setAmountToStepBy(0.01);
    scaleFactor.addAspect(PropertyPreferencesAdapterTag.getInstance());

    this.segmentCount =
      new AspectedIntegerProperty(this, "segment count", segmentCount);
    this.segmentCount.addAspect(BoundedIntegerTag.getIntegerInstance(),
      new IntegerSpinnerValueFactory(
        MIN_SEGMENTS, MAX_SEGMENTS / segmentMultiplier));
    this.segmentCount.addAspect(LevelOfDetailTag.getInstance()).set(10);
    this.segmentCount.addAspect(PropertyPreferencesAdapterTag.getInstance());
    this.segmentCoords = new CircularTriangleStripSegmentCoordinatesBinding(
      (segmentMultiplier != 1) ?
        this.segmentCount.multiply(segmentMultiplier) :
        this.segmentCount);
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void init()
  {
    adjustPermilleFormat(innerRadius);
    adjustPermilleFormat(outerRadius);
    adjustPercentFormat(scaleFactor);
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
