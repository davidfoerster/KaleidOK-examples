package kaleidok.examples.kaleidoscope.layer;

import be.tarsos.dsp.AudioDispatcher;
import kaleidok.audio.processor.FFTProcessor;
import kaleidok.examples.kaleidoscope.Kaleidoscope;
import processing.core.PApplet;
import processing.core.PImage;


public class SpectrogramLayer extends CircularLayer
{
	private int samplesPerSegment;

  private float[] fftAmplitudes;

	public SpectrogramLayer(PApplet parent, PImage img, int segmentCount, int innerRadius, int outerRadius, FFTProcessor fft)
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);

    fftAmplitudes = fft.amplitudes;
		samplesPerSegment = fftAmplitudes.length / segmentCount;
		assert samplesPerSegment > 0;
	}

  @Override
	public void run()
	{
	  parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
    //parent.noFill();
		//parent.stroke(255, 0, 0);
		//parent.strokeWeight(0.5f);
		parent.noStroke();
		parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		parent.texture(currentImage); // set the texture to use

	  for (int i = 0; i < segmentCount + 1; i++)
	  {
	    int imi = i % segmentCount; // make sure the end equals the start

	    float dynamicOuter = getAvg(imi);
	    //println(dynamicOuter);

	    drawCircleVertex(imi, innerRadius); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(imi, outerRadius * (dynamicOuter + 1)); // draw the vertex using the custom drawVertex() method
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

	private float getAvg(int i)
	{
    final float[] amplitudes = fftAmplitudes;
		final int offset = i * samplesPerSegment;
		float sum = amplitudes[offset];
    for (int j = 1; j < samplesPerSegment; j++)
    	sum += amplitudes[offset + j];
    return sum / samplesPerSegment;
	}
}
