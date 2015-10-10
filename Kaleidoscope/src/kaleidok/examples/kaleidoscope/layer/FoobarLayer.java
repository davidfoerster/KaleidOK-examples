package kaleidok.examples.kaleidoscope.layer;

import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;
import static processing.core.PApplet.map;


/**
 * Draws a ring with varying per-segment radii, which follow a Perlin noise
 * function.
 *
 * @see PApplet#noise(float)
 */
public class FoobarLayer extends CircularImageLayer
{
	public FoobarLayer(PApplet parent, int segmentCount,
    float innerRadius, float outerRadius)
	{
		super(parent, segmentCount, innerRadius, outerRadius);
    setScaleFactor(0.5f);
	}


  /**
   * Sets the distance of the innermost vertices from the centre (after the
   * application of the noise function).
   *
   * @param innerRadius  A radius
   */
  @Override
  public void setInnerRadius( float innerRadius )
  {
    super.setInnerRadius(innerRadius);
    updateIntermediates();
  }


  /**
   * Sets the distance of the outermost vertices from the centre (after the
   * application of the noise function).
   *
   * @param outerRadius  A radius
   */
  @Override
  public void setOuterRadius( float outerRadius )
  {
    super.setOuterRadius(outerRadius);
    updateIntermediates();
  }


  /**
   * Sets, how far into the ring both the outer and inner vertices will
   * oscillate.
   * <p>
   * 0 mean no oscillation; 1 means until the other inner or outer
   * border of the ring respectively. Values outside of that range are possible
   * and result in vertices outside of the ring as defined by the outer and
   * inner radii.
   *
   * @param scaleFactor  An oscillation strength factor (normally between 0 and 1)
   */
  @Override
  public void setScaleFactor( float scaleFactor )
  {
    super.setScaleFactor(scaleFactor);
    updateIntermediates();
  }


  private float innerOffset, outerOffset, innerScale;

  private void updateIntermediates()
  {
    innerOffset = getInnerRadius() / getOuterRadius();
    outerOffset = innerOffset + (1 - innerOffset) * (1 - getScaleFactor());
    innerScale = (1 - innerOffset) * getScaleFactor();
  }


  public void run()
	{
    final PApplet parent = this.parent;
	  final float
      innerOffset = this.innerOffset,
      outerOffset = this.outerOffset,
      innerScale = this.innerScale,
      outerScale = 1 - outerOffset,
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
        outerScaled = map(getScaleFactor(), 1, 0, getInnerRadius(), getOuterRadius()),
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
	    float dynamicInner = parent.noise(fc1 + im);
	    float dynamicOuter = parent.noise(fc2 + im);

      drawCircleVertex(im, dynamicInner * innerScale + innerOffset);
      drawCircleVertex(im, dynamicOuter * outerScale + outerOffset);
	    //drawCircleVertex(im, map(dynamicInner, 0, 1, innerOffset, innerOffset + (getOuterRadius() - getInnerRadius()) / getOuterRadius() * getScaleFactor()));
	    //drawCircleVertex(im, map(dynamicOuter, 0, 1, innerOffset + (getOuterRadius() - getInnerRadius()) / getOuterRadius() * (1 - getScaleFactor()), 1));
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

}
