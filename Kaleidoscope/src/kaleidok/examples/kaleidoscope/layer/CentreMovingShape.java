package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.examples.kaleidoscope.Kaleidoscope;
import kaleidok.processing.PImageFuture;
import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.wireframe;

public class CentreMovingShape extends CircularLayer
{
  private int imageIndex;

  private VolumeLevelProcessor volumeLevelProcessor;

  public CentreMovingShape( Kaleidoscope parent, PImageFuture img, int segmentCount, int radius,
    VolumeLevelProcessor volumeLevelProcessor )
  {
    super(parent, img, segmentCount, 0, radius);
    this.volumeLevelProcessor = volumeLevelProcessor;

    imageIndex = (int) parent.random(parent.images.size());
    if (img == null)
      currentImage = parent.images.get(imageIndex);
  }

  public void run()
  {
    double level = volumeLevelProcessor.getLevel();
    //System.out.println("Volume level: " + level);
    float radius = outerRadius * (float) Math.pow(level, 0.5) * 4f;

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.translate(parent.width / 2f, parent.height / 2f); // translate to the left-center
    parent.rotate(parent.frameCount * -0.002f); // rotate around this center --anticlockwise

    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    PImage img;
    if (wireframe < 1 && (img = currentImage.getNoThrow()) != null) {
      parent.texture(img); // set the texture to use
      parent.noStroke(); // turn off stroke
    } else {
      parent.noFill();
      parent.stroke(128);
      parent.strokeWeight(0.5f);
    }

    parent.vertex(0, 0, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i = 0; i <= segmentCount; i++) {
      drawCircleVertex(i % segmentCount, radius); // make sure the end equals the start & draw the vertex using the custom drawVertex() method
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }

  public void nextImage()
  {
    Kaleidoscope parent = (Kaleidoscope) this.parent;
    imageIndex = (imageIndex + 1) % parent.images.size();
    currentImage = parent.images.get(imageIndex);
  }

}
