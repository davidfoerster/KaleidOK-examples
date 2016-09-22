package kaleidok.util;

import java.util.function.Function;
import java.util.regex.Matcher;
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


  public static CharSequence replaceAll( Pattern pattern, CharSequence input,
    Function<? super Matcher, ?> replacer )
  {
    Matcher matcher = pattern.matcher(input);
    if (!matcher.find())
      return input;

    StringBuilder buf = new StringBuilder();
    int lastMatchEnd = 0;
    do
    {
      Object replacement = replacer.apply(matcher);
      if (replacement == null)
        break;

      buf.append(input, lastMatchEnd, matcher.start());

      if (replacement instanceof CharSequence)
      {
        buf.append((CharSequence) replacement);
      }
      else if (replacement instanceof char[])
      {
        buf.append((char[]) replacement);
      }
      else if (replacement instanceof Character)
      {
        buf.append(((Character) replacement).charValue());
      }
      else
      {
        buf.append(replacement);
      }

      lastMatchEnd = matcher.end();
    }
    while (matcher.find());

    return buf.append(input, lastMatchEnd, input.length());
  }


  private static final Pattern CAMEL_CASE_CONVERSION_PATTERN =
    Pattern.compile("[\\s-]+(\\p{javaLowerCase})?");


  public static CharSequence toCamelCase( CharSequence name )
  {
    return replaceAll(CAMEL_CASE_CONVERSION_PATTERN, name,
      ( matcher ) -> {
        String group1 = matcher.group(1);
        return
          (group1 == null) ? "" :
          (matcher.start() == 0) ? group1 :
            Character.toUpperCase(group1.charAt(0));
      });
  }


  public static boolean isInteger( CharSequence s )
  {
    if (s.length() == 0)
      return false;

    char c0 = s.charAt(0);
    final int start = (c0 == '+' || c0 == '-') ? 1 : 0;
    for (int i = s.length() - 1; i >= start; i--)
    {
      if (!Character.isDigit(s.charAt(i)))
        return false;
    }
    return true;
  }
}
