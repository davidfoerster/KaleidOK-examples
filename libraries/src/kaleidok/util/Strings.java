package kaleidok.util;

import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.log;


public final class Strings
{
  private Strings() { }


  public static boolean startsWithToken( String s, String prefix, char delimiter )
  {
    return s.startsWith(prefix) &&
      (s.length() == prefix.length() || s.charAt(prefix.length()) == delimiter);
  }


  private static char toDigit( int n )
  {
    return (char) (n + (n < 10 ? '0' : 'a' - 10));
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
      dst[i] = toDigit((int) n & 0xf);
    java.util.Arrays.fill(dst, offset, i + 1, '0');
    return dst;
  }


  public static final int DIGITS_BASE_MAX = 36;

  public static char[] toDigits( long n, int base, char[] dst )
  {
    if (dst == null) {
      int len = (int) ceil(log(abs((double) n)) / log(base));
      if (n < 0)
        len++;
      dst = new char[len];
    }
    return toDigits(n, base, dst, 0, dst.length);
  }

  public static char[] toDigits( long n, long base, char[] dst, int offset, int len )
  {
    assert base > 1 && base <= DIGITS_BASE_MAX && len > 0 :
      String.format("base %d is not in [2, %d] or length %d ≤ 0",
        base, DIGITS_BASE_MAX, len);

    boolean negative = n < 0;
    if (negative) {
      if (n == Long.MIN_VALUE) {
        dst[offset + (--len)] = toDigit((int) -(n % base));
        n /= base;
      }
      n = -n;
    }

    int i;
    for (i = offset + len - 1; i >= offset && n != 0; i--, n /= base)
      dst[i] = toDigit((int)(n % base));

    if (i >= offset) {
      if (negative)
        dst[offset++] = '-';
      java.util.Arrays.fill(dst, offset, i + 1, '0');
    }

    return dst;
  }


  public static boolean isAscii( int c )
  {
    return (c & ~0x7f) == 0;
  }

  public static boolean isAscii( CharSequence s )
  {
    for (int i = 0, len = s.length(); i < len; i++)
    {
      if (!isAscii(s.charAt(i)))
        return false;
    }
    return true;
  }


  public static boolean isConcatenation( String s, String prefix,
    String suffix )
  {
    return s.length() == prefix.length() + suffix.length() &&
      s.startsWith(prefix) && s.endsWith(suffix);
  }


  public static final Pattern SIMPLE_URL_PREFIX_PATTERN =
    Pattern.compile("^[a-z](?:[a-z0-9+.-])*:");

  public static boolean looksLikeUrl( CharSequence s )
  {
    return SIMPLE_URL_PREFIX_PATTERN.matcher(s).matches();
  }
}
