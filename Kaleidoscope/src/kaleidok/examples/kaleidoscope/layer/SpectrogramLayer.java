package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.spectrum.LinearAverageSpectrum;
import kaleidok.audio.spectrum.Spectrum;
import processing.core.PApplet;
import processing.core.PImage;


public class SpectrogramLayer extends CircularLayer
{
	private final LinearAverageSpectrum avgSpectrum;

	public SpectrogramLayer( PApplet parent, PImage img, int segmentCount,
    int innerRadius, int outerRadius, Spectrum spectrum )
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);
		avgSpectrum = new LinearAverageSpectrum(spectrum);
    avgSpectrum.setBands(segmentCount);
	}

  @Override
	public void run()
	{
    if (!avgSpectrum.update())
      return;

	  parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
    //parent.noFill();
		//parent.stroke(255, 0, 0);
		//parent.strokeWeight(0.5f);
		parent.noStroke();
		parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		parent.texture(currentImage); // set the texture to use

    float bandsPerBin = (float)((double) avgSpectrum.getSize() / avgSpectrum.spectrum.getSize());
	  for (int i = 0; i <= segmentCount; i++)
	  {
	    int imi = i % segmentCount; // make sure the end equals the start

	    float dynamicOuter = avgSpectrum.get(imi) * bandsPerBin;
	    //println(dynamicOuter);

	    drawCircleVertex(imi, innerRadius); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(imi, outerRadius * (dynamicOuter + 1)); // draw the vertex using the custom drawVertex() method
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}
}
