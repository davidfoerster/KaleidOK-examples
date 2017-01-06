package kaleidok.image.filter;

import java.awt.Color;
import java.awt.image.RGBImageFilter;


public abstract class HSBImageFilter extends RGBImageFilter
{
  private float[] hsbBuf = new float[3];


  public abstract boolean isNeutral();


  public abstract float[] filterHSB( int x, int y, float[] hsb );


  @Override
  public int filterRGB( int x, int y, int rgb )
  {
    float[] transformedHSB = filterHSB(x, y, Color.RGBtoHSB(
      (rgb >>> 16) & 0xff, (rgb >>> 8) & 0xff, rgb & 0xff, hsbBuf));
    return Color.HSBtoRGB(
      transformedHSB[0], transformedHSB[1], transformedHSB[2]);
  }


  @Override
  public HSBImageFilter clone()
  {
    HSBImageFilter copy = (HSBImageFilter) super.clone();
    copy.hsbBuf = copy.hsbBuf.clone();
    return copy;
  }
}
