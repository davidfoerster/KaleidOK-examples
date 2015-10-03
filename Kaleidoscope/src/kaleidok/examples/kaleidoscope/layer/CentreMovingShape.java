package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.examples.kaleidoscope.Kaleidoscope;
import kaleidok.examples.kaleidoscope.LayerManager;
import kaleidok.processing.PImageFuture;
import kaleidok.util.CyclingList;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.List;

import static kaleidok.util.DebugManager.wireframe;

public class CentreMovingShape extends CircularLayer
{
  private final CyclingList<PImageFuture> images;

  private VolumeLevelProcessor volumeLevelProcessor;


  public CentreMovingShape( Kaleidoscope parent, List<PImageFuture> images,
    int segmentCount, float radius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    this(parent, new CyclingList<>(images, (int) parent.random(images.size())),
      segmentCount, radius, volumeLevelProcessor);
  }


  private CentreMovingShape( Kaleidoscope parent,
    CyclingList<PImageFuture> images, int segmentCount, float radius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    super(parent, images.getNext(), segmentCount, 0, radius);
    this.images = images;
    this.volumeLevelProcessor = volumeLevelProcessor;
  }


  public void run()
  {
    final PApplet parent = this.parent;
    final double level = volumeLevelProcessor.getLevel();
    //System.out.println("Volume level: " + level);
    final float radius = (float) Math.pow(level, 0.5) * getScaleFactor();

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.translate(parent.width / 2f, parent.height / 2f); // translate to the left-center
    parent.scale(getOuterRadius());
    parent.rotate(parent.frameCount * -0.002f); // rotate around this center --anticlockwise

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
      drawCircleVertex(i % segmentCount, radius);
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }


  public void cycleImage()
  {
    setNextImage(images.getNext());
  }

}
