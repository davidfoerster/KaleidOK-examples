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

  public double exponent = 1.125;


	public SpectrogramLayer( PApplet parent, PImageFuture img, int segmentCount,
    int innerRadius, int outerRadius, MinimFFTProcessor spectrum )
	{
		super(parent, img, segmentCount * 2, innerRadius, outerRadius);
		setScaleFactor(5e-3f);
    avgSpectrum = spectrum;

    double nyquistFreq = 22050 / 2;
    int minFreq = 86;
    avgSpectrum.logAverages(minFreq, (int) ceil(segmentCount / log2(nyquistFreq / minFreq)));
	}


  public float expectedMaximumSpectrum = 60;

  public float scaleSpectralLine( double x )
  {
    return (float) pow(x, exponent) * getScaleFactor();
  }


  @Override
	public void run()
	{
    if (!avgSpectrum.isReady())
      return;

    assert getSegmentCount() <= avgSpectrum.size();

	  parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
    final float
      outerScale = 1 + scaleSpectralLine(expectedMaximumSpectrum),
      totalScale = getOuterRadius() * outerScale,
      innerRadiusScaled = getInnerRadius() / totalScale;
    parent.scale(totalScale);

		PImage img;
		if (wireframe < 1 && (img = getCurrentImage()) != null) {
      parent.noStroke();
      parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
      parent.texture(img); // set the texture to use
    } else {
      parent.noFill();
      parent.stroke(255, 0, 0);
      parent.strokeWeight(0.5f / totalScale);

      parent.ellipseMode(PApplet.RADIUS);
      parent.ellipse(0, 0, 1, 1);

      parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
    }

    final int segmentCount = getSegmentCount();
	  for (int i = 0; i <= segmentCount; i += 2)
	  {
	    int imi = i % segmentCount; // make sure the end equals the start

	    float dynamicOuter = 1 + scaleSpectralLine(avgSpectrum.get(imi / 2));
	    //System.out.println(dynamicOuter);

	    drawCircleVertex(imi, innerRadiusScaled); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(imi + 1, dynamicOuter / outerScale); // draw the vertex using the custom drawVertex() method
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}
}
