package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;
import static processing.core.PApplet.map;


public class FoobarLayer extends CircularLayer
{
  private float radiusRatio;


	public FoobarLayer(PApplet parent, PImageFuture img, int segmentCount,
    float innerRadius, float outerRadius)
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);
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

    if (debug >= 1 && wireframe >= 1)
    {
      // draw the ring borders
      parent.stroke(0, 255, 255);
      drawDebugCircle(getInnerRadius());
      parent.stroke(255, 255, 0);
      drawDebugCircle(getOuterRadius());

      float
        innerScaled = map(getScaleFactor(), 0, 1, getInnerRadius(), getOuterRadius()),
        outerScaled = map(getScaleFactor(), 1, 0, getOuterRadius(), getInnerRadius()),
        scaleDiff = outerScaled - innerScaled;
      // make sure, that the inner circles don't lie on top of each other
      if (Math.abs(scaleDiff) < 2f) {
        float avgScaled = (outerScaled + innerScaled) / 2,
          offset = Math.copySign(1f, scaleDiff);
        innerScaled = avgScaled - offset;
        outerScaled = avgScaled + offset;
      }
      // draw the extents of the inner and outer noise rings
      parent.stroke(0, 128, 128);
      drawDebugCircle(innerScaled);
      parent.stroke(128, 128, 0);
      drawDebugCircle(outerScaled);
    }

		parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
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
