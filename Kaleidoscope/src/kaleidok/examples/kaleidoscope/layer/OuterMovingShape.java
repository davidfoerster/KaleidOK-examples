package kaleidok.examples.kaleidoscope.layer;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PImage;


/**
 * Draws a shape that is rotated with a speed depending on the logarithm of the
 * pitch frequency of an audio signal. The rough relation between the two is:
 * <pre>
 * v = (a * log(pitchFrequency) + b) / frameRate
 * </pre>
 * Where <code>a</code> and <code>b</code> are some suitable (currently
 * hard-coded) values and <code>v</code> is the angular velocity.
 *
 * @see be.tarsos.dsp.pitch.PitchProcessor
 */
public class OuterMovingShape extends CircularImageLayer
{
  private double angle = 0, step = 0;


  public OuterMovingShape( ExtPApplet parent, int segmentCount, float radius )
  {
    super(parent, segmentCount, 0, radius);
  }


  public void run()
  {
    final PApplet parent = this.parent;

    if (wireframe >= 2) {
      parent.stroke(192, 0, 0);
      drawDebugCircle(getOuterRadius());
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(getOuterRadius());

    if (step != 0) {
      /*
       * Set the angle of the total rotation, with "step" as the rotation since
       * the last drawn frame. For numerical stability we wrap around the angle
       * after a full rotation (2π).
       */
      angle = (angle + step) % (Math.PI * 2);
    }
    parent.rotate((float) angle); // rotate around this center

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
      drawCircleVertex(i % segmentCount, 1);
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  private PitchDetectionHandler pitchDetectionHandler = null;

  /**
   * @return The pitch detection handler of this shape
   * @see be.tarsos.dsp.pitch.PitchProcessor#PitchProcessor(PitchProcessor.PitchEstimationAlgorithm, float, int, PitchDetectionHandler)
   */
  public PitchDetectionHandler getPitchDetectionHandler()
  {
    if (pitchDetectionHandler == null)
      pitchDetectionHandler = new MyPitchDetectionHandler();
    return pitchDetectionHandler;
  }


  private class MyPitchDetectionHandler implements PitchDetectionHandler
  {
    private long lastPitchDetectionTime = -1;

    @Override
    public void handlePitch( PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent )
    {
      if (lastPitchDetectionTime >= 0 || pitchDetectionResult.isPitched())
      {
        long now = System.nanoTime();

        //if (lastPitchDetectionTime >= 0) {
          //System.out.format("Pitch lasted for %d ms.\n", (int)(now - lastPitchDetectionTime) / 1000000);
        //}

        if (pitchDetectionResult.isPitched())
        {
          float pitch = pitchDetectionResult.getPitch();
          float stepDeg = PApplet.map((float) Math.log(pitch), 3.5f, 8.5f, -3f, 3f);
          /*System.out.format("Pitch: %.0f Hz, %.0f %%, %s; step = %.2f°\n",
            pitch, pitchDetectionResult.getProbability() * 100f, pitchDetectionResult.isPitched(), stepDeg);*/
          step = Math.toRadians(stepDeg);
          lastPitchDetectionTime = now;
        }
        else
        {
          step = 0;
          lastPitchDetectionTime = -1;
        }
      }
    }
  }
}
