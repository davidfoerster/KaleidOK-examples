package chromatik;

import java.util.Arrays;


final class Utils
{
  private Utils() { }

  public static float square( float x )
  {
    return x * x;
  }

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
