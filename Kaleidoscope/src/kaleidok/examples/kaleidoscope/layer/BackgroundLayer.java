package kaleidok.examples.kaleidoscope.layer;

import kaleidok.processing.ExtPApplet;
import processing.core.PApplet;
import processing.core.PImage;

import static kaleidok.util.DebugManager.wireframe;


public class BackgroundLayer extends ImageLayer
{
  public BackgroundLayer( PApplet parent )
  {
    super(parent);
  }


  @Override
  public void run()
  {
    PImage bgImage;
    if (wireframe < 1 && (bgImage = getCurrentImage()) != null) {
      // background image
      final ExtPApplet parent = (ExtPApplet) this.parent;
      parent.image(bgImage, ExtPApplet.ImageResizeMode.PAN, 0, 0,
        parent.width, parent.height); // resize-display image correctly to cover the whole screen
      parent.fill(255, 125 + (float) Math.sin(parent.frameCount * 0.01) * 5); // white fill with dynamic transparency
      parent.rect(0, 0, parent.width, parent.height); // rect covering the whole canvas
    } else {
      parent.background(0);
    }
  }
}
