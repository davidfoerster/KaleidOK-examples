package kaleidok.examples.kaleidoscope.layer;

import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.wireframe;


public class FoobarLayer extends CircularImageLayer
{
  private float radiusRatio;


	public FoobarLayer(PApplet parent, int segmentCount,
    float innerRadius, float outerRadius)
	{
		super(parent, segmentCount, innerRadius, outerRadius);
	}


  @Override
  public void setInnerRadius( float innerRadius )
  {
    super.setInnerRadius(innerRadius);
    updateRadiusRatio();
  }


  @Override
  public void setOuterRadius( float outerRadius )
  {
    super.setOuterRadius(outerRadius);
    updateRadiusRatio();
  }


  private void updateRadiusRatio()
  {
    radiusRatio = getInnerRadius() / getOuterRadius();
  }


  public void run()
	{
    final PApplet parent = this.parent;
	  final float
      radiusRatio = this.radiusRatio,
      fc1 = parent.frameCount * 0.01f,
	    fc2 = parent.frameCount * 0.02f;

		parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width / 2f, parent.height / 2f); // translate to the right-center
		parent.scale(getOuterRadius());

		parent.stroke(255); // set stroke to white
    parent.strokeWeight(1 / getOuterRadius());
		parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		PImage img;
		if (wireframe < 1 && (img = getCurrentImage()) != null) {
			parent.texture(img); // set the texture to use
		} else {
			parent.noFill();
		}

		final int segmentCount = getSegmentCount();
	  for (int i = 0; i <= segmentCount; i++) {
	    int im = i % segmentCount; // make sure the end equals the start

	    // each vertex has a noise-based dynamic movement
	    float dynamicInner = (parent.noise(fc1 + im) * 2) + 1;
	    float dynamicOuter = (parent.noise(fc2 + im) + 2) / 3;

	    drawCircleVertex(im, dynamicInner * radiusRatio);
	    drawCircleVertex(im, dynamicOuter);
	  }

    /*
    for (int i = 0; i <= segmentCount; i += 2) {
      int im = i % segmentCount; // make sure the end equals the start

      // each vertex has a noise-based dynamic movement
      float dynamicInner = (parent.noise(fc1 + im) * 2) + 1;
      float dynamicOuter = (parent.noise(fc2 + (im + 1)) + 2) / 3;

      drawCircleVertex(im, dynamicInner * innerRatio);
      drawCircleVertex(im + 1, dynamicOuter);
    }
   */

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

}
