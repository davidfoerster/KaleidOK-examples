package kaleidok.util;

import java.util.*;


public final class Strings
{
  private Strings() { }

  public static String join( Collection<? extends CharSequence> ar, char separator )
  {
    if (ar.isEmpty())
      return "";

    Iterator<? extends CharSequence> it = ar.iterator();
    if (ar.size() == 1)
      return it.next().toString();

    int len = ar.size() - 1;
    while (it.hasNext())
      len += it.next().length();
    StringBuilder sb = new StringBuilder(len);

    it = ar.iterator();
    sb.append(it.next());
    while (it.hasNext())
      sb.append(separator).append(it.next());

    return sb.toString();
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
  public static char[] toHexDigits( long n, char[] dst )
  {
    if (dst == null)
      dst = new char[Long.SIZE / 4];
    return toHexDigits(n, dst, 0, dst.length);
  }

  public static char[] toHexDigits( long n, char[] dst, int offset, int len )
  {
    int i;
    for (i = offset + len - 1; i >= offset && n != 0; i--, n >>>= 4)
      dst[i] = Character.forDigit((int) n & 0xf, 16);
    java.util.Arrays.fill(dst, offset, i + 1, '0');
    return dst;
  }
}
