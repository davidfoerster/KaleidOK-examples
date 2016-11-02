package kaleidok.kaleidoscope.layer;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import static java.lang.Math.log;
import static java.lang.Math.toRadians;
import static processing.core.PApplet.map;


/**
 * Draws a shape that is rotated with a speed depending on the logarithm of the
 * pitch frequency of an audio signal. The rough relation between the two is:
 * <pre>
 * v = (a * log(pitchFrequency) + b) / frameRate
 * </pre>
 * Where {@code a} and {@code b} are some suitable (currently hard-coded)
 * values and {@code v} is the angular velocity.
 *
 * @see PitchProcessor
 */
public class OuterMovingShape extends CircularImageLayer
{
  private double angle = 0, step = 0;


  public OuterMovingShape( ExtPApplet parent, int segmentCount, double radius )
  {
    super(parent, segmentCount);
    this.outerRadius.set(radius);
  }


  @Override
  public void run()
  {
    final PApplet parent = this.parent;
    final int wireframe = this.wireframe.get();
    final float outerRadius = (float) this.outerRadius.get();

    if (wireframe >= 2) {
      parent.stroke(192, 0, 0);
      drawDebugCircle(outerRadius);
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(outerRadius);

    if (step != 0) {
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
    for (int i = 0; i <= segmentCount; i++) {
      drawCircleVertex(i % segmentCount, 1);
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  /**
   * The pitch detection handler of this shape
   * @see PitchProcessor#PitchProcessor(PitchEstimationAlgorithm, float, int, PitchDetectionHandler)
   */
  @SuppressWarnings("Convert2Lambda")
  public final PitchDetectionHandler pitchDetectionHandler =
    new PitchDetectionHandler()
    {
      @Override
      public void handlePitch( PitchDetectionResult pitchDetectionResult,
        AudioEvent audioEvent )
      {
        step = pitchDetectionResult.isPitched() ?
          toRadians(map(
            (float) log(pitchDetectionResult.getPitch()), 3f, 7f, -3f, 3f)) :
          0;
      }
    };
}
