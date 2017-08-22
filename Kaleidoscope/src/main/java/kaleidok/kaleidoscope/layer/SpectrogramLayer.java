package kaleidok.kaleidoscope.layer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.kaleidoscope.layer.util.SpectrumBandsPerOctaveBinding;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import static java.lang.Math.pow;
import static kaleidok.kaleidoscope.layer.util.LayerUtils.adjustFormat;


/**
 * Draws a ring whose outer edge forms a spectrogram of an audio (or other
 * one-dimensional) signal.
 *
 * @see MinimFFTProcessor
 */
public class SpectrogramLayer extends CircularImageLayer
{
  private static final int SEGMENT_MULTIPLIER = 2;

  private final MinimFFTProcessor avgSpectrum;

  // Keep this field around to prevent the garbage collection on the weak reference of the binding.
  @SuppressWarnings("FieldCanBeLocal")
  private final SpectrumBandsPerOctaveBinding bandsPerOctaveBinding;

  /**
   * Manages the exponent to adjust the dynamic range of the spectral
   * intensities.
   */
  protected final AspectedDoubleProperty exponent;


  public SpectrogramLayer( ExtPApplet parent, int segmentCount,
    double innerRadius, double outerRadius, MinimFFTProcessor spectrum,
    double sampleRate )
  {
    super(parent, segmentCount, SEGMENT_MULTIPLIER);
    this.innerRadius.set(innerRadius);
    this.outerRadius.set(outerRadius);
    this.scaleFactor
      .getAspect(BoundedDoubleTag.<DoubleSpinnerValueFactory>getDoubleInstance())
      .setAmountToStepBy(0.0025);

    avgSpectrum = spectrum;
    bandsPerOctaveBinding =
      new SpectrumBandsPerOctaveBinding(this.segmentCount, sampleRate);
    bandsPerOctaveBinding.attach(spectrum);

    exponent = new AspectedDoubleProperty(this, "exponent", 1.125);
    exponent
      .addAspect(BoundedDoubleTag.getDoubleInstance(),
        new DoubleSpinnerValueFactory(0, 4))
      .setAmountToStepBy(0.05);
    exponent.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  @Override
  public void init()
  {
    super.init();
    adjustFormat(exponent);
  }


  /**
   * Manages the scale factor for spectral intensities. You should set this to
   * the inverse of the largest expected spectral intensity.
   *
   * @return  A property object with the above purpose
   */
  @SuppressWarnings({ "RedundantMethodOverride", "EmptyMethod" })
  @Override
  public DoubleProperty scaleFactorProperty()
  {
    return super.scaleFactorProperty();
  }


  public DoubleProperty exponentProperty()
  {
    return exponent;
  }


  private final DoubleBinding scaledInnerRadius = innerRadius.divide(outerRadius);


  /**
   * Draws the current spectrogram around a ring. The spectral lines are scaled
   * according to a power function:
   * <pre>
   * l = (a ⋅ x)<sup>exponent</sup>
   * </pre>
   * where {@code a} is the result of
   * <code>{@link #scaleFactorProperty()}</code>.
   * <p>
   * {@code l} is more or less assumed to lie between 0 and 1, which holds true
   * for {@code x} between 0 and {@code a} and a non-negative {@code exponent}.
   *
   * @see #exponent
   * @see #scaleFactorProperty()
   */
  @Override
  public void run()
  {
    final PApplet parent = this.parent;
    final int wireframe = this.wireframe.get();
    final float outerRadius = this.outerRadius.floatValue();

    if (wireframe >= 2)
    {
      parent.stroke(0, 192, 0);
      drawDebugCircle(innerRadius.floatValue());
      parent.stroke(128, 255, 128);
      drawDebugCircle(outerRadius);
    }

    if (!avgSpectrum.isReady())
      return;

    final MinimFFTProcessor avgSpectrum = this.avgSpectrum;
    final float
      scaledInnerRadius = this.scaledInnerRadius.floatValue(),
      outerScale = 1 - scaledInnerRadius,
      scaleFactor = this.scaleFactor.floatValue();
    final double exponent = this.exponent.get();
    final int segmentCount = this.segmentCount.get();
    /*
    assert segmentCount <= avgSpectrum.size() :
      segmentCount + " > " + avgSpectrum.size();
    */

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(outerRadius);

    PImage img;
    if (wireframe <= 0 && (img = updateAndGetCurrentImage()) != null)
    {
      parent.noStroke();
      parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
      parent.texture(img); // set the texture to use
    }
    else
    {
      parent.noFill();
      parent.stroke(0, 255, 0);
      parent.strokeWeight(parent.g.strokeWeight * 0.5f / outerRadius);
      parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
    }

    for (int i = 0; i <= segmentCount; i++)
    {
      final int im = i % segmentCount; // make sure the end equals the start

      float x = avgSpectrum.get(im);
      // scale the intensity value and adjust its dynamic range:
      float dynamicOuter = (float) pow(x * scaleFactor, exponent);

      drawCircleVertex(im * SEGMENT_MULTIPLIER,
        scaledInnerRadius);
      drawCircleVertex(im * SEGMENT_MULTIPLIER + 1,
        dynamicOuter * outerScale + scaledInnerRadius);
    }

    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }
}
