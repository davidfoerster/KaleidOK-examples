package kaleidok.processing.image;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;


public enum ImageResizeMode
{
  @SuppressWarnings("unused")
  STRETCH
  {
    @Override
    public void drawImage( PApplet p, PImage img,
      float x1, float y1, float x2, float y2 )
    {
      drawImageNormalTextureCoords(p, img, x1, y1, x2, y2, 0, 0, 1, 1);
    }
  },


  ZOOM
  {
    @Override
    public void drawImage( PApplet p, PImage img,
      float x1, float y1, float x2, float y2 )
    {
      float u1 = 0, v1 = 0, u2 = 1, v2 = 1;
      float drawRatio = (x2 - x1) / (y2 - y1);
      float imgRatio = (float) img.width / img.height;
      if (imgRatio > drawRatio)
      {
        float halfNormWidth = drawRatio / imgRatio * 0.5f;
        u1 = 0.5f - halfNormWidth;
        u2 = 0.5f + halfNormWidth;
      }
      else if (imgRatio < drawRatio)
      {
        float halfNormHeight = imgRatio / drawRatio * 0.5f;
        v1 = 0.5f - halfNormHeight;
        v2 = 0.5f + halfNormHeight;
      }

      drawImageNormalTextureCoords(p, img, x1, y1, x2, y2, u1, v1, u2, v2);
    }
  };


  public abstract void drawImage( PApplet p, PImage img,
    float x1, float y1, float x2, float y2 );


  protected static void drawImageNormalTextureCoords( PApplet p, PImage img,
    float x1, float y1, float x2, float y2,
    float u1, float v1, float u2, float v2 )
  {
    boolean savedStroke = p.g.stroke;
    int savedStrokeColor = p.g.strokeColor;
    p.noStroke();
    int savedTextureMode = p.g.textureMode;
    p.textureMode(PConstants.NORMAL);

    p.beginShape(PConstants.QUADS);
    p.texture(img);
    p.vertex(x1, y1, u1, v1);
    p.vertex(x1, y2, u1, v2);
    p.vertex(x2, y2, u2, v2);
    p.vertex(x2, y1, u2, v1);
    p.endShape();

    p.textureMode(savedTextureMode);
    if (savedStroke)
      p.stroke(savedStrokeColor);
  }
}
