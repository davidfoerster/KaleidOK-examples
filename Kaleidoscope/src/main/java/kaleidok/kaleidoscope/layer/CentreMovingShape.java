package kaleidok.kaleidoscope.layer;

import javafx.beans.property.DoubleProperty;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.processing.ExtPApplet;
import kaleidok.util.CyclingList;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.List;
import java.util.concurrent.Future;

import static java.lang.Math.pow;
import static kaleidok.kaleidoscope.layer.util.LayerUtils.adjustFormat;
import static kaleidok.util.Math.mapNormalized;


/**
 * Draws a disc whose radius depends on a volume level. The disc and its
 * texture are rotated at a constant speed.
 *
 * @see VolumeLevelProcessor
 */
public class CentreMovingShape extends CircularImageLayer
{
  private final CyclingList<? extends Future<PImage>> images;

  private VolumeLevelProcessor volumeLevelProcessor;

  /**
   * Manages the exponent to adjust the dynamic range of the spectral
   * intensities.
   *
   * @see #exponentProperty()
   */
  protected final AspectedDoubleProperty exponent;


  public CentreMovingShape( ExtPApplet parent,
    List<? extends Future<PImage>> images,
    int segmentCount, double innerRadius, double outerRadius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    this(parent, new CyclingList<>(images, (int) parent.random(images.size())),
      segmentCount, innerRadius, outerRadius, volumeLevelProcessor);
  }


  private CentreMovingShape( ExtPApplet parent,
    CyclingList<? extends Future<PImage>> images,
    int segmentCount, double innerRadius, double outerRadius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    super(parent, segmentCount);
    this.innerRadius.set(innerRadius);
    this.outerRadius.set(outerRadius);

    setNextImage(images.getNext());
    this.images = images;
    this.volumeLevelProcessor = volumeLevelProcessor;

    exponent = new AspectedDoubleProperty(this, "exponent", 0.5);
    exponent
      .addAspect(BoundedDoubleTag.getDoubleInstance(), new DoubleSpinnerValueFactory(0, 4))
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
   * Manages the exponent for the dynamic range adjustment of the volume level
   * before deriving the radius:
   * <pre>
   * radius = (level ⋅ scale)<sup>exp</sup>
   * </pre>
   * where {@code level} is assumed to lie between 0 and 1, so that the same
   * can be true for {@code radius}.
   * <p>
   * See {@link #run()} for more details on the relation between the
   * {@link #innerRadiusProperty() radius}, the
   * {@link VolumeLevelProcessor#getLevel() volume level}, the
   * {@link #scaleFactorProperty() scale factor}, and the exponent.
   *
   * @return  A property object with the above purpose
   * @see #exponent
   */
  public DoubleProperty exponentProperty()
  {
    return exponent;
  }


  /**
   * Draws the rotated circle with a volume-dependent radius. The volume level
   * is scaled according to a power function:
   * <pre>
   * l = (a ⋅ x)<sup>exponent</sup>
   * </pre>
   * where {@code a} is the value of
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
    double level = volumeLevelProcessor.getLevel();
    //System.out.println("Volume level: " + level);
    if (!(level > 0))
      return;

    // Adjust the dynamic range of the volume level:
    level = pow(level * scaleFactor.get(), exponent.get());
    final float
      outerRadius = this.outerRadius.floatValue(),
      radius = mapNormalized(
        (float) level, this.innerRadius.floatValue(), outerRadius);

    final PApplet parent = this.parent;
    final int wireframe = this.wireframe.get();
    if (wireframe >= 2) {
      parent.stroke(128, 0, 128);
      drawDebugCircle(outerRadius);
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(outerRadius);
    parent.rotate(parent.frameCount * -0.002f); // rotate around this center --anticlockwise

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
      drawCircleVertex(i, radius);
    }
    drawCircleVertex(0, radius);

    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  public void cycleImage()
  {
    setNextImage(images.getNext());
  }

}
