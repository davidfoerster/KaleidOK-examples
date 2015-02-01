package chromatik;

import java.util.Arrays;


final class Utils
{
  private Utils() { }

  public static float square( float x )
  {
    return x * x;
  }

  /**
   * Writes the hexadecimal representation of an integer to a char array. If
   * the array is too short to hold all necessary digits, it'll only contain
   * the least significant digits. If the array can hold more digits than
   * necessary, it'll be padded with zero digits from the left.
   *
   * If {@code dst} is {@code null}, a char array of length 16 (enough to hold
   * any {@code long}) will be allocated.
   *
   * @param n  An integer
   * @param dst  A char array to hold the hexadecimal digits
   * @return  A char array with the hexadecimal representation of {@code n}
   */
  public static char[] toHex( long n, char[] dst )
  {
    if (dst == null)
      dst = new char[Long.SIZE / 4];
    int i;
    for (i = dst.length - 1; i >= 0 && n != 0; i--, n >>>= 4)
      dst[i] = Character.forDigit((int) n & 0xf, 16);
    Arrays.fill(dst, 0, i + 1, '0');
    return dst;
  }
}
