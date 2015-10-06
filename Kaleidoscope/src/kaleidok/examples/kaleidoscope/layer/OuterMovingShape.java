package kaleidok.examples.kaleidoscope.layer;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;


public class OuterMovingShape extends CircularLayer
{
  private double angle = 0, step = 0;


  public OuterMovingShape( PApplet parent, PImageFuture img,
    int segmentCount, float radius )
  {
    super(parent, img, segmentCount, 0, radius);
  }


  public void run()
  {
    final PApplet parent = this.parent;

    if (debug >= 1 && wireframe >= 1) {
      parent.stroke(192, 0, 0);
      drawDebugCircle(getOuterRadius());
    }

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.scale(getOuterRadius());

    if (step != 0) {
      angle = (angle + step) % (Math.PI * 2);
    }
    parent.rotate((float) angle); // rotate around this center

    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    PImage img;
    if (wireframe < 1 && (img = getCurrentImage()) != null) {
      parent.texture(img); // set the texture to use
      parent.noStroke(); // turn off stroke
    } else {
      parent.noFill();
      parent.stroke(128);
      parent.strokeWeight(0.5f / getOuterRadius());
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

        if (lastPitchDetectionTime >= 0) {
          //System.out.format("Pitch lasted for %d ms.\n", (int)(now - lastPitchDetectionTime) / 1000000);
        }

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
