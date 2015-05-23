package kaleidok.examples.kaleidoscope.layer;

import processing.core.PApplet;
import processing.core.PImage;


public class FoobarLayer extends CircularLayer
{

	public FoobarLayer(PApplet parent, PImage img, int segmentCount, int innerRadius, int outerRadius)
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);
	}

	public void run()
	{
		// calculate fc1 and fc2 once per draw(), since they are used for the dynamic movement of many vertices
	  float fc1 = parent.frameCount * 0.01f;
	  float fc2 = parent.frameCount * 0.02f;

		parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
		parent.noStroke();
		parent.stroke(255); // set stroke to white
		parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		parent.texture(currentImage); // set the texture to use
	  for (int i = 0; i <= segmentCount; i++) {
	    int im = i % segmentCount; // make sure the end equals the start

	    // each vertex has a noise-based dynamic movement
	    float dynamicInner = 0.5f + parent.noise(fc1 + im); //replace with audio analysis
	    float dynamicOuter = 0.5f + parent.noise(fc2 + im);

	    drawCircleVertex(im, innerRadius * dynamicInner); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(im, outerRadius * dynamicOuter); // draw the vertex using the custom drawVertex() method
	  }
		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

}
