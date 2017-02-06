package kaleidok.kaleidoscope.layer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.property.DoubleProperty;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.util.converter.CachingFormattedStringConverter;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import sun.misc.FloatConsts;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static kaleidok.util.Math.mapNormalized;


/**
 * Draws a ring with varying per-segment radii, which follow a Perlin noise
 * function.
 *
 * @see PApplet#noise(float)
 */
public class FoobarLayer extends CircularImageLayer
{
  private final AspectedDoubleProperty innerNoiseFrequency, outerNoiseFrequency;


  public FoobarLayer( ExtPApplet parent, int segmentCount,
    double innerRadius, double outerRadius )
  {
    super(parent, segmentCount);
    this.innerRadius.set(innerRadius);
    this.outerRadius.set(outerRadius);
    this.scaleFactor.set(0.5);

    innerNoiseFrequency =
      makeNoiseFrequencyProperty("inner noise frequency", 4e-4);
    outerNoiseFrequency =
      makeNoiseFrequencyProperty("outer noise frequency", 8e-4);
  }


  private AspectedDoubleProperty makeNoiseFrequencyProperty( String name,
    double initialValue )
  {
    AspectedDoubleProperty p =
      new AspectedDoubleProperty(this, name, initialValue);
    DoubleSpinnerValueFactory bounds = new DoubleSpinnerValueFactory(0, 1);
    bounds.setAmountToStepBy(1e-4 / 2);
    DecimalFormat fmt = (DecimalFormat) NumberFormat.getNumberInstance();
    fmt.setMultiplier(1000);
    bounds.setConverter(new CachingFormattedStringConverter<>(fmt));
    p.addAspect(BoundedDoubleTag.getDoubleInstance(), bounds);
    p.addAspect(PropertyPreferencesAdapterTag.getInstance());
    return p;
  }


  public DoubleProperty innerNoiseFrequencyProperty()
  {
    return innerNoiseFrequency;
  }

  public DoubleProperty outerNoiseFrequencyProperty()
  {
    return outerNoiseFrequency;
  }


  /**
   * Manages the distance of the innermost vertices from the centre (after the
   * application of the noise function).
   *
   * @return  A property object with the above purpose
   */
  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public DoubleProperty innerRadiusProperty()
  {
    return super.innerRadiusProperty();
  }


  /**
   * Sets the distance of the outermost vertices from the centre (after the
   * application of the noise function).
   *
   * @return  A property object with the above purpose
   */
  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public DoubleProperty outerRadiusProperty()
  {
    return super.outerRadiusProperty();
  }


  /**
 * Manager how far into the ring both the outer and inner vertices will
 * oscillate.
 * <p>
 * 0 mean no oscillation; 1 means until the other inner or outer
 * border of the ring respectively. Values outside of that range are possible
 * and result in vertices outside of the ring as defined by the outer and
 * inner radii.
 *
 * @return  A property object with the above purpose
 */
  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public DoubleProperty scaleFactorProperty()
  {
    return super.scaleFactorProperty();
  }


  private final DoubleBinding innerOffset = innerRadius.divide(outerRadius);


  private final FloatBinding outerOffset =
    Bindings.createFloatBinding(
      () -> {
          float innerOffset = FoobarLayer.this.innerOffset.floatValue();
          return innerOffset +
            (1 - innerOffset) * (1 - scaleFactor.floatValue());
        },
      innerOffset, scaleFactor);


  private final FloatBinding innerScale =
    Bindings.createFloatBinding(
      () -> (1 - innerOffset.floatValue()) * scaleFactor.floatValue(),
      innerOffset, scaleFactor);


  @Override
  public void run()
  {
    final PApplet parent = this.parent;
    final int wireframe = this.wireframe.get();
    final float
      innerRadius = this.innerRadius.floatValue(),
      outerRadius = this.outerRadius.floatValue(),
      innerOffset = this.innerOffset.floatValue(),
      outerOffset = this.outerOffset.get(),
      innerScale = this.innerScale.get(),
      outerScale = 1 - outerOffset;

    final float
      // Wrap around the run time every ~1.1 h but preserve noise precision after that time
      runTime = parent.millis() & (FloatConsts.SIGNIF_BIT_MASK >>> 1),
      fc1 = runTime * innerNoiseFrequency.floatValue(),
      fc2 = runTime * outerNoiseFrequency.floatValue();

    if (wireframe >= 2)
    {
      // draw the ring borders
      parent.stroke(0, 255, 255);
      drawDebugCircle(innerRadius);
      parent.stroke(255, 255, 0);
      drawDebugCircle(outerRadius);

      float
        scaleFactor = this.scaleFactor.floatValue(),
        innerScaled = mapNormalized(scaleFactor, innerRadius, outerRadius),
        //innerScaled = map(scaleFactor, 0, 1, innerRadius, outerRadius),
        outerScaled = mapNormalized(scaleFactor, outerRadius, innerRadius),
        //outerScaled = map(scaleFactor, 1, 0, innerRadius, outerRadius),
        scaleDiff = outerScaled - innerScaled;
      // make sure, that the inner circles don't lie on top of each other
      if (Math.abs(scaleDiff) < 2f) {
        float avgScaled = (outerScaled + innerScaled) * 0.5f,
          offset = Math.copySign(1f, scaleDiff);
        innerScaled = avgScaled - offset;
        outerScaled = avgScaled + offset;
      }
      // draw the extents of the inner and outer noise rings
      parent.stroke(0, 128, 128);
      drawDebugCircle(innerScaled);
      parent.stroke(128, 128, 0);
      drawDebugCircle(outerScaled);
    }

    parent.pushMatrix(); // use push/popMatrix so each shape translation does not affect other drawings
    parent.scale(outerRadius);

    parent.stroke(255); // set stroke to white
    parent.strokeWeight(parent.g.strokeWeight / outerRadius);
    parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
    PImage img;
    if (wireframe <= 0 && (img = updateAndGetCurrentImage()) != null)
    {
      parent.texture(img); // set the texture to use
    }
    else
    {
      parent.noFill();
    }

    final int segmentCount = this.segmentCount.get();
    for (int i = 0; i <= segmentCount; i++) {
      int im = i % segmentCount; // make sure the end equals the start

      // each vertex has a noise-based dynamic movement
      float dynamicInner = parent.noise(fc1 + im);
      float dynamicOuter = parent.noise(fc2 + im);

      drawCircleVertex(im, dynamicInner * innerScale + innerOffset);
      drawCircleVertex(im, dynamicOuter * outerScale + outerOffset);
      //drawCircleVertex(im, map(dynamicInner, 0, 1, innerOffset, innerOffset + (getOuterRadius() - getInnerRadius()) / getOuterRadius() * getScaleFactor()));
      //drawCircleVertex(im, map(dynamicOuter, 0, 1, innerOffset + (getOuterRadius() - getInnerRadius()) / getOuterRadius() * (1 - getScaleFactor()), 1));
    }

    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }

}
