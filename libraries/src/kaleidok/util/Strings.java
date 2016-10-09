package kaleidok.util;

import java.util.regex.*;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static kaleidok.util.AssertionUtils.fastAssert;


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
    Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE);

  public static boolean looksLikeUrl( CharSequence s )
  {
    return SIMPLE_URL_PREFIX_PATTERN.matcher(s).find();
  }


  @FunctionalInterface
  public interface ReplaceCallback
  {
    enum Replacement
    {
      /**
       * Replace the current match with itself.
       */
      DONT_REPLACE,

      /**
       * Like {@link #DONT_REPLACE} but don't perform any further matches. The
       * remainder of the input sequence is <em>left as it is</em>.
       */
      BREAK,

      /**
       * Like {@link #DONT_REPLACE} but don't perform any further matches. The
       * remainder of the input sequence is <em>deleted</em>.
       */
      STOP,

      /**
       * Abort the entire replacement operation.
       * {@link #replaceAll(Pattern, CharSequence, ReplaceCallback)} will return
       * {@code null}.
       */
      ABORT
    }


    Object apply( MatchResult matchResult, CharSequence input,
      StringBuilder resultBuffer );
  }


  /**
   * Finds all occurrences of {@code pattern} in {@code input} and replaces
   * them according to the return value of the {@code replacer} callback.
   *
   * @param pattern  A regular expression
   * @param input  The input character sequence (never modified)
   * @param replacer  A callback function to determine the replacement string
   *   each match. Instances of {@link CharSequence}, {@code char[]}, and
   *   {@link Character} are replaced with an equivalent of their respective
   *   canonical string representation. Instances of
   *   {@link ReplaceCallback.Replacement} receive special handling.
   * @return  The substituted character sequence
   * @throws NullPointerException  if any arguments or any callback return
   *   value is {@code null}.
   * @throws ClassCastException  if the type of a callback value is not among
   *   described above
   * @see ReplaceCallback.Replacement
   */
  @SuppressWarnings("ProhibitedExceptionDeclared")
  public static CharSequence replaceAll( Pattern pattern, CharSequence input,
    ReplaceCallback replacer )
  throws NullPointerException, ClassCastException
  {
    Matcher matcher = pattern.matcher(input);
    StringBuilder buf = null;

    int lastMatchEnd = 0;
    matcherLoop:
    while (matcher.find())
    {
      Object replacement = replacer.apply(matcher, input, buf);

      if (replacement instanceof ReplaceCallback.Replacement)
      {
        switch ((ReplaceCallback.Replacement) replacement)
        {
        case DONT_REPLACE:
          continue matcherLoop;

        case BREAK:
          break matcherLoop;

        case ABORT:
          return null;
        }
      }

      if (buf == null)
        buf = new StringBuilder(matcher.start() + 16);

      buf.append(input, lastMatchEnd, matcher.start());

      if (replacement == ReplaceCallback.Replacement.STOP)
      {
        return buf;
      }
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
      else if (replacement != null)
      {
        throw new ClassCastException(
          "Unsupported replacement callback value type " +
            replacement.getClass().getName());
      }
      else
      {
        throw new NullPointerException("replacement callback value");
      }

      lastMatchEnd = matcher.end();
    }

    return (buf != null) ?
      buf.append(input, lastMatchEnd, input.length()) :
      input;
  }


  private static final Pattern CAMEL_CASE_CONVERSION_PATTERN =
    Pattern.compile("[\\s-]+(\\p{javaLowerCase}?)");


  public static CharSequence toCamelCase( CharSequence s )
  {
    return (s.length() < 2) ?
      s :
      replaceAll(CAMEL_CASE_CONVERSION_PATTERN, s,
        (matcher, input, sb) -> {
          int start1 = matcher.start(1), end1 = matcher.end(1);
          if (start1 == end1)
            return "";
          fastAssert(start1 + 1 == end1);
          char c = input.charAt(start1);
          return (matcher.start() != 0) ? Character.toUpperCase(c) : c;
        });
  }


  public static boolean isInteger( CharSequence s )
  {
    final int len = s.length();
    if (len == 0)
      return false;

    char c0 = s.charAt(0);
    if (!Character.isDigit(c0) && (len == 1 || (c0 != '+' && c0 != '-')))
      return false;

    for (int i = 1; i < len; i++)
    {
      if (!Character.isDigit(s.charAt(i)))
        return false;
    }
    return true;
  }
}
