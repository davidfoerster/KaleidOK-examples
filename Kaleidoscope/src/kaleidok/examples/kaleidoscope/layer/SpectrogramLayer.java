package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;

import static java.lang.Math.ceil;
import static java.lang.Math.pow;
import static kaleidok.util.DebugManager.wireframe;
import static kaleidok.util.Math.log2;


public class SpectrogramLayer extends CircularLayer
{
	private final MinimFFTProcessor avgSpectrum;

	public SpectrogramLayer( PApplet parent, PImageFuture img, int segmentCount,
    int innerRadius, int outerRadius, MinimFFTProcessor spectrum )
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);
    avgSpectrum = spectrum;

    double nyquistFreq = 22050 / 2;
    int minFreq = 86;
    avgSpectrum.logAverages(minFreq, (int) ceil(segmentCount / log2(nyquistFreq / minFreq)));
	}

  @Override
	public void run()
	{
    if (!avgSpectrum.isReady())
      return;

    assert segmentCount <= avgSpectrum.size();

	  parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
    parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		PImage img;
		if (wireframe < 1 && (img = currentImage.getNoThrow()) != null) {
      parent.noStroke();
      parent.texture(img); // set the texture to use
    } else {
      parent.noFill();
      parent.stroke(255, 0, 0);
      parent.strokeWeight(0.5f);
    }

	  for (int i = 0; i <= segmentCount; i++)
	  {
	    int imi = i % segmentCount; // make sure the end equals the start

	    float dynamicOuter = (float) pow(avgSpectrum.get(imi), 1.125f) * 5e-3f;
	    //System.out.println(dynamicOuter);

	    drawCircleVertex(imi, innerRadius); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(imi, outerRadius * (dynamicOuter + 1)); // draw the vertex using the custom drawVertex() method
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}
}
