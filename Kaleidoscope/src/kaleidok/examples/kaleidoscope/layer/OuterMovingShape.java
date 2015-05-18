package kaleidok.examples.kaleidoscope.layer;

import kaleidok.audio.processor.VolumeLevelProcessor;
import processing.core.PApplet;
import processing.core.PImage;


public class OuterMovingShape extends CircularLayer
{
  private float angle = 0;

  private final VolumeLevelProcessor volumeLevelProcessor;

  public OuterMovingShape(PApplet parent, PImage img, int segmentCount, int radius,
    VolumeLevelProcessor volumeLevelProcessor)
  {
    super(parent, img, segmentCount, 0, radius);
    this.volumeLevelProcessor = volumeLevelProcessor;
  }

  public void run()
  {
    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.translate(parent.width / 2f, parent.height / 2f); // translate to the left-center

    double level = volumeLevelProcessor.getLevel();
    float step = (float) level;
    angle = (angle + step) % PApplet.TWO_PI;
    parent.rotate(angle); // rotate around this center

    parent.noStroke(); // turn off stroke
    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    parent.texture(currentImage); // set the texture to use
    parent.vertex(0, 0, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i=0; i<segmentCount+1; i++) {
      drawCircleSegment(i % segmentCount); // make sure the end equals the start & draw the vertex using the custom drawVertex() method
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }
}
