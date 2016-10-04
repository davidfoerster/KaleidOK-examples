package kaleidok.kaleidoscope.layer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.javafx.beans.property.SimpleBoundedDoubleProperty;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import static java.lang.Math.ceil;
import static java.lang.Math.pow;
import static kaleidok.util.Math.log2;


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

  /**
   * Manages the exponent to adjust the dynamic range of the spectral
   * intensities.
   */
  protected final SimpleBoundedDoubleProperty exponent =
    new SimpleBoundedDoubleProperty(this, "exponent", 1.125, 0, 4, 0.05);

  private static final int MIN_FREQUENCY = 86;


  public SpectrogramLayer( ExtPApplet parent, int segmentCount,
    double innerRadius, double outerRadius, MinimFFTProcessor spectrum,
    float sampleRate )
  {
    super(parent, segmentCount, SEGMENT_MULTIPLIER);
    this.innerRadius.set(innerRadius);
    this.outerRadius.set(outerRadius);
    this.scaleFactor.getBounds().setAmountToStepBy(0.0025);

    avgSpectrum = spectrum;

    float nyquistFreq = sampleRate / 2;
    avgSpectrum.logAverages(MIN_FREQUENCY,
      (int) ceil(segmentCount / log2(nyquistFreq / MIN_FREQUENCY)));
  }


  @Override
  public void init()
  {
    super.init();
    LayerUtils.adjustFormat(exponent);
  }


  /**
   * Manages the scale factor for spectral intensities. You should set this to
   * the inverse of the largest expected spectral intensity.
   *
   * @return  A property object with the above purpose
   */
  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public DoubleProperty scaleFactorProperty()
  {
    return super.scaleFactorProperty();
  }


  public DoubleProperty exponentProperty()
  {
    return exponent;
  }


  private final NumberBinding scaledInnerRadius =
    Bindings.divide(innerRadius, outerRadius);


  /**
   * Draws the current spectrogram around a ring. The spectral lines are scaled
   * according to a power function:
   * <pre>
   * l = (a * x) ^ exponent
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
    final float outerRadius = (float) this.outerRadius.get();

    if (wireframe >= 2)
    {
      parent.stroke(0, 192, 0);
      drawDebugCircle((float) innerRadius.get());
      parent.stroke(128, 255, 128);
      drawDebugCircle(outerRadius);
    }

    if (!avgSpectrum.isReady())
      return;

    final MinimFFTProcessor avgSpectrum = this.avgSpectrum;
    final float
      scaledInnerRadius = this.scaledInnerRadius.floatValue(),
      outerScale = 1 - scaledInnerRadius,
      scaleFactor = (float) this.scaleFactor.get();
    final double exponent = this.exponent.get();
    final int segmentCount = this.segmentCount.get();
    assert segmentCount <= avgSpectrum.size() :
      segmentCount + " > " + avgSpectrum.size();

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(outerRadius);

    PImage img;
    if (wireframe <= 0 && (img = getCurrentImage()) != null) {
      parent.noStroke();
      parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
      parent.texture(img); // set the texture to use
    } else {
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
