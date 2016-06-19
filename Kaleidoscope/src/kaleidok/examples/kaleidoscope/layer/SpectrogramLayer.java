package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.MinimFFTProcessor;
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
  private final MinimFFTProcessor avgSpectrum;

  private double exponent = 1.125;

  private static final int MIN_FREQUENCY = 86;


  public SpectrogramLayer( ExtPApplet parent, int segmentCount,
    float innerRadius, float outerRadius, MinimFFTProcessor spectrum,
    float sampleRate )
  {
    super(parent);
    init(segmentCount, innerRadius, outerRadius);
    avgSpectrum = spectrum;

    float nyquistFreq = sampleRate / 2;
    avgSpectrum.logAverages(MIN_FREQUENCY,
      (int) ceil(segmentCount / log2(nyquistFreq / MIN_FREQUENCY)));
  }


  @Override
  public int getSegmentCount()
  {
    return super.getSegmentCount() / 2;
  }

  @Override
  public void setSegmentCount( int segmentCount )
  {
    /*
     * Double the segment count to give the spectrogram area a nicely falling
     * off outer curve instead of straight normal lines.
     */
    super.setSegmentCount(segmentCount * 2);
  }


  /**
   * Set the scale factor for spectral intensities. You should set this to the
   * inverse of the largest expected spectral intensity.
   *
   * @param scaleFactor  A scale factor
   * @see #run()
   */
  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public void setScaleFactor( float scaleFactor )
  {
    super.setScaleFactor(scaleFactor);
  }


  /**
   * @return  The current exponent for dynamic range adjustments
   * @see #setExponent(double)
   */
  public double getExponent()
  {
    return exponent;
  }

  /**
   * Sets the exponent to adjust the dynamic range of the spectral intensities.
   *
   * @param exponent  An exponent
   * @see #run()
   */
  public void setExponent( double exponent )
  {
    this.exponent = exponent;
  }


  @Override
  public void setOuterRadius( float outerRadius )
  {
    super.setOuterRadius(outerRadius);
    updateIntermediates();
  }

  @Override
  public void setInnerRadius( float innerRadius )
  {
    super.setInnerRadius(innerRadius);
    updateIntermediates();
  }


  private float scaledInnerRadius;

  private void updateIntermediates()
  {
    scaledInnerRadius = getInnerRadius() / getOuterRadius();
  }


  /**
   * Draws the current spectrogram around a ring. The spectral lines are scaled
   * according to a power function:
   * <pre>
   * l = (a * x) ^ exponent
   * </pre>
   * where {@code a} is the result of {@code {@link #getScaleFactor()}}.
   * <p>
   * {@code l} is more or less assumed to lie between 0 and 1, which holds true
   * for {@code x} between 0 and {@code a} and a non-negative {@code exponent}.
   *
   * @see #getExponent()
   * @see #getScaleFactor()
   */
  @Override
  public void run()
  {
    final PApplet parent = this.parent;

    if (wireframe >= 2)
    {
      parent.stroke(0, 192, 0);
      drawDebugCircle(getInnerRadius());
      parent.stroke(128, 255, 128);
      drawDebugCircle(getOuterRadius());
    }

    if (!avgSpectrum.isReady())
      return;

    final MinimFFTProcessor avgSpectrum = this.avgSpectrum;
    final float scaledInnerRadius = this.scaledInnerRadius,
      outerScale = 1 - scaledInnerRadius,
      scaleFactor = getScaleFactor();
    final double exponent = getExponent();
    final int segmentCount = super.getSegmentCount();
    assert segmentCount <= avgSpectrum.size() :
      segmentCount + " > " + avgSpectrum.size();

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(getOuterRadius());

    PImage img;
    if (wireframe <= 0 && (img = getCurrentImage()) != null) {
      parent.noStroke();
      parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
      parent.texture(img); // set the texture to use
    } else {
      parent.noFill();
      parent.stroke(0, 255, 0);
      parent.strokeWeight(parent.g.strokeWeight * 0.5f / getOuterRadius());
      parent.beginShape(PConstants.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
    }

    for (int i = 0; i <= segmentCount; i += 2)
    {
      int imi = i % segmentCount; // make sure the end equals the start

      float x = avgSpectrum.get(imi / 2);
      // scale the intensity value and adjust its dynamic range:
      float dynamicOuter = (float) pow(x * scaleFactor, exponent);

      drawCircleVertex(imi, scaledInnerRadius);
      drawCircleVertex(imi + 1, dynamicOuter * outerScale + scaledInnerRadius);
    }

    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }
}
