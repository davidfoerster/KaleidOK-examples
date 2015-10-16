package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.examples.kaleidoscope.Kaleidoscope;
import kaleidok.processing.PImageFuture;
import kaleidok.util.CyclingList;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.List;

import static java.lang.Math.pow;


/**
 * Draws a whose radius depends on a volume level. The disc and its texture
 * are rotated at a constant speed.
 *
 * @see VolumeLevelProcessor
 */
public class CentreMovingShape extends CircularImageLayer
{
  private final CyclingList<PImageFuture> images;

  private VolumeLevelProcessor volumeLevelProcessor;

  private double exponent = 0.5;


  public CentreMovingShape( Kaleidoscope parent, List<PImageFuture> images,
    int segmentCount, float innerRadius, float outerRadius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    this(parent, new CyclingList<>(images, (int) parent.random(images.size())),
      segmentCount, innerRadius, outerRadius, volumeLevelProcessor);
  }


  private CentreMovingShape( Kaleidoscope parent,
    CyclingList<PImageFuture> images, int segmentCount, float innerRadius,
    float outerRadius, VolumeLevelProcessor volumeLevelProcessor )
  {
    super(parent, segmentCount, innerRadius, outerRadius);
    setNextImage(images.getNext());
    this.images = images;
    this.volumeLevelProcessor = volumeLevelProcessor;
  }


  /**
   * Sets the exponent for the dynamic range adjustment of the volume level
   * before deriving the radius:
   * <pre>
   * radius = level ^ exp
   * </pre>
   * where <code>level</code> is assumed to lie between 0 and 1, so that
   * the same can be true for <code>radius</code>.
   *
   * @param exp  An exponent
   */
  @Override
  public void setScaleFactor( float exp )
  {
    super.setScaleFactor(exp);
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


  /**
   * Draws the rotated circle with a volume-dependent radius. The volume level
   * is scaled according to a power function:
   * <pre>
   * l = (a * x) ^ exponent
   * </pre>
   * where <code>a</code> is the result of
   * <code>{@link #getScaleFactor()}</code>.
   * <p>
   * <code>l</code> is more or less assumed to lie between 0 and 1, which
   * holds true for <code>x</code> between 0 and <code>a</code> and a
   * non-negative <code>exponent</code>.
   *
   * @see #getExponent()
   * @see #getScaleFactor()
   */
  @Override
  public void run()
  {
    double level = volumeLevelProcessor.getLevel();
    //System.out.println("Volume level: " + level);
    if (!(level > 0))
      return;

    // Adjust the dynamic range of the volume level:
    level = pow(level * getScaleFactor(), getExponent());
    final float radius =
      ((float) level * (getOuterRadius() - getInnerRadius()) + getInnerRadius()) / getOuterRadius();

    final PApplet parent = this.parent;
    if (wireframe >= 2) {
      parent.stroke(128, 0, 128);
      drawDebugCircle(getOuterRadius());
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(getOuterRadius());
    parent.rotate(parent.frameCount * -0.002f); // rotate around this center --anticlockwise

    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    PImage img;
    if (wireframe <= 0 && (img = getCurrentImage()) != null) {
      parent.texture(img); // set the texture to use
      parent.noStroke(); // turn off stroke
    } else {
      parent.noFill();
      parent.stroke(128);
      parent.strokeWeight(parent.g.strokeWeight * 0.5f / getOuterRadius());
    }

    final int segmentCount = getSegmentCount();
    parent.vertex(0, 0, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i = 0; i <= segmentCount; i++) {
      drawCircleVertex(i % segmentCount, radius);
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  public void cycleImage()
  {
    setNextImage(images.getNext());
  }

}
