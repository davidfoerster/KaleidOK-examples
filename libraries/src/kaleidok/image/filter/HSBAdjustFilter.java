package kaleidok.image.filter;

import kaleidok.util.function.BinaryFloatFunction;

import static kaleidok.util.Math.clamp;


public class HSBAdjustFilter extends HSBImageFilter
{
  public float hue, saturation, brightness;

  public BinaryFloatFunction filterMode;


  public HSBAdjustFilter( float hue, float saturation, float brightness,
    BinaryFloatFunction filterMode )
  {
    this.hue = hue;
    this.saturation = saturation;
    this.brightness = brightness;
    this.filterMode = filterMode;
    canFilterIndexColorModel = true;
  }


  public HSBAdjustFilter( float hue, float saturation, float brightness )
  {
    this(hue, saturation, brightness, (left, right) -> left + right);
  }


  public HSBAdjustFilter()
  {
    this(0, 0, 0);
  }


  @Override
  public float[] filterHSB( int x, int y, float[] hsb )
  {
    hsb[0] = filterMode.applyAsFloat(hsb[0], hue) % (float)(Math.PI * 2);
    hsb[1] = clamp(filterMode.applyAsFloat(hsb[1], saturation), 0, 1);
    hsb[2] = clamp(filterMode.applyAsFloat(hsb[2], brightness), 0, 1);
    return hsb;
  }
}
