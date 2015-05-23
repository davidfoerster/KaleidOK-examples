package kaleidok.examples.kaleidoscope.layer;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import kaleidok.examples.kaleidoscope.Kaleidoscope;
import processing.core.PApplet;
import processing.core.PImage;


public class OuterMovingShape extends CircularLayer
{
  private double angle = 0, step = 0;

  public OuterMovingShape(PApplet parent, PImage img, int segmentCount, int radius,
    AudioDispatcher audioDispatcher)
  {
    super(parent, img, segmentCount, 0, radius);

    PitchProcessor pitchProcessor =
      new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(), Kaleidoscope.audioBufferSize,
        new PitchDetectionHandler());
    audioDispatcher.addAudioProcessor(pitchProcessor);
  }

  public void run()
  {
    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.translate(parent.width / 2f, parent.height / 2f); // translate to the left-center

    if (step != 0) {
      angle = (angle + step) % (Math.PI * 2);
    }
    parent.rotate((float) angle); // rotate around this center

    parent.noStroke(); // turn off stroke
    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    parent.texture(currentImage); // set the texture to use
    parent.vertex(0, 0, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i = 0; i <= segmentCount; i++) {
      drawCircleSegment(i % segmentCount); // make sure the end equals the start & draw the vertex using the custom drawVertex() method
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }

  private class PitchDetectionHandler implements be.tarsos.dsp.pitch.PitchDetectionHandler
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
