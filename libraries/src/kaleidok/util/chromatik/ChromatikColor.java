package kaleidok.util.chromatik;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static kaleidok.util.chromatik.Utils.square;


/**
 * Represents one of Chromatik’s preset colors.
 */
public class ChromatikColor
{
  /**
   * RGB color value
   */
  public final int value;

  /**
   * Name of the color group as required by Chromatik’s search engine
   */
  public final String groupName;

  /**
   * Construct color object by a compound RGB value
   *
   * @param rgb  An RGB color value
   * @see #ChromatikColor(int, int, int)
   */
  public ChromatikColor( int rgb )
  {
    this((rgb >>> 16) & 0xff, (rgb >>> 8) & 0xff, rgb & 0xff);
  }

  /**
   * Construct color object from individual RGB values (0-255).
   * The resulting color object will contain the most similar color of the set
   * of Chromatik’s preset colors.
   *
   * @param r  Red color component
   * @param g  Green color component
   * @param b  Blue color component
   */
  public ChromatikColor( int r, int g, int b )
  {
    float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);

    if (hsb[1] < SATURATION_BINS[0] / (255 * 2))
    {
      // color is so desaturated that we'll map it to a gray scale color.

      /*
       * Find brightness bin index (black = 0, white = hueNames.length-1).
       * Chromatik's darkest accepted gray value is 21 (roughly 255/12).
       */
      int brBin = max(round(hsb[2] * HUE_COLS), 1);

      // and go back to gray value
      int c = brBin * 255 / HUE_COLS;
      assert c >= 21 && c <= 255;
      value = c | c << 8 | c << 16;

      // find color group name based on rules
      groupName =
        GRAYSCALE_NAMES[ (brBin <= 1) ? 0 : (brBin < HUE_COLS - 1) ? 1 : 2 ];
    }
    else
    {
      /*
       * Round to a bin and wrap around hue value (so that pinkish red maps to
       * red at bin 0).
       */
      int hueBin = round(hsb[0] * HUE_COLS) % HUE_COLS;

      /*
       * The row of in Chromatik's color selector is the bin index with the
       * closest distance in the vector space of saturation and brightness.
       */
      float sat = hsb[1] * 255, br = hsb[2] * 255;
      float min = Float.POSITIVE_INFINITY;
      int row = -1;
      for (int i = SATURATION_BINS.length - 1; i >= 0; i--) {
        float d =
          square(sat - SATURATION_BINS[i]) +
          square(br - BRIGHTNESS_BINS[i]);
        if (d < min) {
          min = d;
          row = i;
        }
      }

      value = COLORS[row * HUE_COLS + hueBin];
      groupName = HUE_NAMES[hueBin];
    }
  }

  @Override
  public int hashCode()
  {
    return value;
  }

  @Override
  public boolean equals( Object obj )
  {
    return obj instanceof ChromatikColor && ((ChromatikColor) obj).value == this.value;
  }

  @Override
  public String toString()
  {
    char[] a = new char[groupName.length() + 9];
    Utils.toHex(value, a, 0, 6);
    a[6] = ' ';
    a[7] = '(';
    groupName.getChars(0, groupName.length(), a, 8);
    a[a.length - 1] = ')';

    return new String(a);
  }

  static final String[] HUE_NAMES = {
    "Red", "Orange", "Yellow", "Green", "Green", "Green",
    "Cyan", "Blue", "Blue", "Purple", "Pink", "Pink"
  };

  static final String[] GRAYSCALE_NAMES = {
    "Black", "Gray", "White"
  };

  static final int HUE_COLS = HUE_NAMES.length;

  static final float[] SATURATION_BINS = {
    52, 107, 166, 227, 227, 228, 228
  };

  static final float[] BRIGHTNESS_BINS = {
    248, 242, 235, 229, 172, 114, 57
  };

  static final int[] COLORS = {
    0xf8c5c5, 0xf8dfc5, 0xf8f8c5, 0xdff8c5, 0xc5f8c5, 0xc5f8df,
    0xc5f8f8, 0xc5dff8, 0xc5c5f8, 0xdfc5f8, 0xf8c5f8, 0xf8c5df,

    0xf28c8c, 0xf2bf8c, 0xf2f28c, 0xbff28c, 0x8cf28c, 0x8cf2bf,
    0x8cf2f2, 0x8cbff2, 0x8c8cf2, 0xbf8cf2, 0xf28cf2, 0xf28cbf,

    0xeb5252, 0xeb9f52, 0xebeb52, 0x9feb52, 0x52eb52, 0x52eb9f,
    0x52ebeb, 0x529feb, 0x5252eb, 0x9f52eb, 0xeb52eb, 0xeb529f,

    0xe51919, 0xe57f19, 0xe5e519, 0x7fe519, 0x19e519, 0x19e57f,
    0x19e5e5, 0x197fe5, 0x1919e5, 0x7f19e5, 0xe519e5, 0xe5197f,

    0xac1313, 0xac5f13, 0xacac13, 0x5fac13, 0x13ac13, 0x13ac5f,
    0x13acac, 0x135fac, 0x1313ac, 0x5f13ac, 0xac13ac, 0xac135f,

    0x720c0c, 0x723f0c, 0x72720c, 0x3f720c, 0x0c720c, 0x0c723f,
    0x0c7272, 0x0c3f72, 0x0c0c72, 0x3f0c72, 0x720c72, 0x720c3f,

    0x390606, 0x391f06, 0x393906, 0x1f3906, 0x063906, 0x06391f,
    0x063939, 0x061f39, 0x060639, 0x1f0639, 0x390639, 0x39061f
  };
}
