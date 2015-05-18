import processing.core.PApplet;
import processing.core.PImage;


class CentreMovingShape extends CircularLayer
{
  private int imageIndex;

  public CentreMovingShape(Kaleidoscope parent, PImage img, int segmentCount, int radius)
  {
    super(parent, img, segmentCount, 0, radius);

    imageIndex = (int) parent.random(parent.images.length);
    if (img == null)
      currentImage = parent.images[imageIndex];
  }

  public void run()
  {
    //float level = audioSource.mix.level();
    float radius = outerRadius;// * pow(level, 0.5) * 4;

    parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
    parent.translate(parent.width / 2, parent.height / 2); // translate to the left-center
    parent.rotate(parent.frameCount * -0.002f); // rotate around this center --anticlockwise
    parent.noStroke(); // turn off stroke
    parent.beginShape(PApplet.TRIANGLE_FAN); // input the shapeMode in the beginShape() call
    parent.texture(currentImage); // set the texture to use
    parent.vertex(10f, 10f, 0.5f, 0.5f); // define a central point for the TRIANGLE_FAN, note the (0.5, 0.5) uv texture coordinates
    for (int i=0; i<segmentCount+1; i++) {
      drawCircleVertex(i % segmentCount, radius); // make sure the end equals the start & draw the vertex using the custom drawVertex() method
    }
    parent.endShape(); // finalize the Shape
    parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
  }

  public void nextImage()
  {
    Kaleidoscope parent = (Kaleidoscope) this.parent;
    imageIndex = (imageIndex + 1) % parent.images.length;
    currentImage = parent.images[imageIndex];
  }

}
