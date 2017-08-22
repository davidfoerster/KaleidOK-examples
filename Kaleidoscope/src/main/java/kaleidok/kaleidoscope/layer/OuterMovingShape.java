package kaleidok.kaleidoscope.layer;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.util.converter.DoubleNumberStringConverter;
import kaleidok.processing.ExtPApplet;
import kaleidok.text.InternationalSystemOfUnitsFormat;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import static kaleidok.util.Math.map;


/**
 * Draws a shape that is rotated with a speed depending on the logarithm of the
 * pitch frequency of an audio signal. The rough relation between the two is:
 * <pre>
 * v = (a ⋅ log(pitchFrequency) + b) ÷ frameRate
 * </pre>
 * Where {@code a} and {@code b} are derived from
 * {@link #pitchToAngleMapMinPitchProperty()},
 * {@link #pitchToAngleMapMaxPitchProperty()},
 * {@link #pitchToAngleMapMinAngleProperty()}, and
 * {@link #pitchToAngleMapMaxAngleProperty()}.
 *
 * @see PitchProcessor
 */
public class OuterMovingShape extends CircularImageLayer
{
  private final AspectedDoubleProperty
    pitchToAngleMapMinPitch,
    pitchToAngleMapMaxPitch,
    pitchToAngleMapMinAngle,
    pitchToAngleMapMaxAngle;

  private final AngleStepSizeBinding angleStepSizeBinding;

  private double angle = 0;


  public OuterMovingShape( ExtPApplet parent, int segmentCount, double radius )
  {
    super(parent, segmentCount);
    this.outerRadius.set(radius);

    AspectedDoubleProperty[] pitchToAngleMapProperties = {
      pitchToAngleMapMinPitch =
        makePitchToAngleMapProperty("min. pitch", " ln(Hz)", 3, 1, 10),
      pitchToAngleMapMaxPitch =
        makePitchToAngleMapProperty("max. pitch", " ln(Hz)", 7, 1, 10),
      pitchToAngleMapMinAngle =
        makePitchToAngleMapProperty("min. angle", "°", -3, -360, 360),
      pitchToAngleMapMaxAngle =
        makePitchToAngleMapProperty("max. angle", "°", +3, -360, 360),
    };
    for (int i = pitchToAngleMapProperties.length - 1; i >= 0; i--)
    {
      pitchToAngleMapProperties[i]
        .addAspect(LevelOfDetailTag.getInstance()).set(i + 1);
    }

    angleStepSizeBinding = new AngleStepSizeBinding(pitchToAngleMapProperties);
  }


  private AspectedDoubleProperty makePitchToAngleMapProperty( String name,
    String unit, double initialValue, double min, double max )
  {
    AspectedDoubleProperty prop =
      new AspectedDoubleProperty(this, "pitch-to-angle map " + name, initialValue);

    DoubleSpinnerValueFactory svf = new DoubleSpinnerValueFactory(min, max);
    svf.setAmountToStepBy(0.25);
    if (unit != null && !unit.isEmpty())
    {
      InternationalSystemOfUnitsFormat fmt =
        InternationalSystemOfUnitsFormat.getNumberInstance(unit);
      fmt.setMagnitudeBounds(0, 0);
      svf.setConverter(new DoubleNumberStringConverter(fmt));
    }

    prop.addAspect(BoundedDoubleTag.getDoubleInstance(), svf);
    prop.addAspect(PropertyPreferencesAdapterTag.getInstance());
    return prop;
  }


  public PitchDetectionHandler getPitchDetectionHandler()
  {
    return angleStepSizeBinding;
  }


  public DoubleProperty pitchToAngleMapMinPitchProperty()
  {
    return pitchToAngleMapMinPitch;
  }

  public double getPitchToAngleMapMinPitch()
  {
    return pitchToAngleMapMinPitch.get();
  }

  public void setPitchToAngleMapMinPitch( double value )
  {
    pitchToAngleMapMaxPitch.set(value);
  }


  public DoubleProperty pitchToAngleMapMaxPitchProperty()
  {
    return pitchToAngleMapMaxPitch;
  }

  public double getPitchToAngleMapMaxPitch()
  {
    return pitchToAngleMapMaxPitch.get();
  }

  public void setPitchToAngleMapMaxPitch( double value )
  {
    pitchToAngleMapMaxPitch.set(value);
  }


  public DoubleProperty pitchToAngleMapMinAngleProperty()
  {
    return pitchToAngleMapMinAngle;
  }

  public double getPitchToAngleMapMinAngle()
  {
    return pitchToAngleMapMinAngle.get();
  }

  public void setPitchToAngleMapMinAngle( double value )
  {
    pitchToAngleMapMaxAngle.set(value);
  }


  public DoubleProperty pitchToAngleMapMaxAngleProperty()
  {
    return pitchToAngleMapMaxAngle;
  }

  public double getPitchToAngleMapMaxAngle()
  {
    return pitchToAngleMapMaxAngle.get();
  }

  public void setPitchToAngleMapMaxAngle( double value )
  {
    pitchToAngleMapMaxAngle.set(value);
  }


  @Override
  public void run()
  {
    final PApplet parent = this.parent;
    final int wireframe = this.wireframe.get();
    final float outerRadius = this.outerRadius.floatValue();

    if (wireframe >= 2) {
      parent.stroke(192, 0, 0);
      drawDebugCircle(outerRadius);
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(outerRadius);

    double step = angleStepSizeBinding.get();
    if (step != 0)
    {
      /*
       * Set the angle of the total rotation, with "step" as the rotation since
       * the last drawn frame. For numerical stability we wrap around the angle
       * after a full rotation (2π).
       */
      angle = (angle + step) % (Math.PI * 2);
    }
    parent.rotate((float) angle); // rotate around this center

    parent.beginShape(PConstants.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    PImage img;
    if (wireframe <= 0 && (img = updateAndGetCurrentImage()) != null)
    {
      parent.texture(img); // set the texture to use
      parent.noStroke(); // turn off stroke
    }
    else
    {
      parent.noFill();
      parent.stroke(128);
      parent.strokeWeight(parent.g.strokeWeight * 0.5f / outerRadius);
    }

    final int segmentCount = this.segmentCount.get();
    parent.vertex(0, 0, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i = 0; i < segmentCount; i++) {
      drawCircleVertex(i, 1);
    }
    drawCircleVertex(segmentCount, 1);

    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  private static class AngleStepSizeBinding extends DoubleBinding
    implements PitchDetectionHandler
  {
    private final ObservableDoubleValue[] bounds;

    private volatile double pitchValue = Double.NaN;


    public AngleStepSizeBinding( ObservableDoubleValue[] bounds )
    {
      bind(bounds);
      this.bounds = bounds;
    }


    @Override
    public void dispose()
    {
      unbind(bounds);
    }


    @Override
    protected double computeValue()
    {
      double pitchValue = this.pitchValue;
      return (pitchValue > 0 && !Double.isNaN(pitchValue)) ?
        Math.toRadians(map(Math.log(pitchValue),
          bounds[0].get(), bounds[1].get(), bounds[2].get(), bounds[3].get())) :
        0;
    }


    @Override
    public void handlePitch( PitchDetectionResult pitchDetectionResult,
      AudioEvent audioEvent )
    {
      pitchValue =
        pitchDetectionResult.isPitched() ?
          pitchDetectionResult.getPitch() :
          Double.NaN;

      invalidate();
    }
  }
}
