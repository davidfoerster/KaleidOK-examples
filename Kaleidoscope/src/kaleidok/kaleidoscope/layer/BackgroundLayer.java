package kaleidok.kaleidoscope.layer;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.image.ImageResizeMode;
import processing.core.PImage;


public class BackgroundLayer extends ImageLayer
{
  public BackgroundLayer( ExtPApplet parent )
  {
    super(parent);
  }


  @Override
  public void run()
  {
    PImage bgImage;
    if (wireframe.get() <= 0 && (bgImage = updateAndGetCurrentImage()) != null)
    {
      final ExtPApplet p = this.parent;
      ImageResizeMode.ZOOM.drawImage(p, bgImage, 0, 0, p.width, p.height); // resize-display image correctly to cover the whole screen
      p.fill(255, 125 + (float) Math.sin(p.frameCount * 0.01) * 5); // white fill with dynamic transparency
      p.rect(0, 0, p.width, p.height); // rect covering the whole canvas
    }
  }
}
