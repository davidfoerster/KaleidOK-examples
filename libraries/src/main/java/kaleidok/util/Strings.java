package kaleidok.util;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.CharBuffer;
import java.util.regex.*;

import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;


public final class Strings
{
  private Strings() { }


  public static boolean startsWithToken( String s, String prefix, char delimiter )
  {
    return s.startsWith(prefix) &&
      (s.length() == prefix.length() || s.charAt(prefix.length()) == delimiter);
  }


  /**
   * @param n  An integer between 0 and 35
   * @return  {@code toDigitUnsafe(n, 'a' - 10)}
   * @see #toDigitUnchecked(int, int)
   */
  public static char toDigitLowerCaseUnchecked( int n )
  {
    return (char) toDigitUnchecked(n, 'a' - 10);
  }


  /**
   * @param n  An integer between 0 and 35
   * @return  {@code toDigitUnsafe(n, 'A' - 10)}
   * @see #toDigitUnchecked(int, int)
   */
  public static char toDigitUpperCaseUnchecked( int n )
  {
    return (char) toDigitUnchecked(n, 'A' - 10);
  }


  /**
   * <p>Converts an integer between 0 and 35 to a suitable digit character:
   * <ul>
   *   <li>0–9 are mapped to {@code '0'}–{@code '9'} and</li>
   *   <li>10–35 are mapped to ({@code letterBase}+10)–({@code letterBase}+35).</li>
   * </ul>
   * </p>
   * <p><em>The result is undefined for integers outside that range.</em></p>
   *
   * @param n  An integer between 0 and 35
   * @param letterBase  A base value for digits in the range [10, 36];
   *   typically {@code 'A'}−10 or {@code 'a'}−10.
   * @return  A digit representing the integer {@code n}
   */
  public static int toDigitUnchecked( int n, int letterBase )
  {
    return (n < 10 ? '0' : letterBase) + n;
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
      dst[i] = toDigitLowerCaseUnchecked((int) n & 0xf);
    java.util.Arrays.fill(dst, offset, i + 1, '0');
    return dst;
  }


  public static char[] toDigits( long n, int base, char[] dst )
  {
    if (base <= 0)
      throw new IllegalArgumentException("Non-positive base");
    if (dst == null) {
      int len =
        (abs(n) > base || n == Long.MIN_VALUE) ?
          (int) ceil(Math.log(abs((double) n), base)) :
          1;
      if (n < 0)
        len++;
      dst = new char[len];
    }
    return toDigits(n, base, dst, 0, dst.length);
  }

  public static char[] toDigits( long n, long base, char[] dst, int offset, int len )
  {
    assert base >= MIN_RADIX && base <= MAX_RADIX && len > 0 :
      String.format("base %d is not in [%d, %d] or length %d ≤ 0",
        base, MIN_RADIX, MAX_RADIX, len);

    boolean negative = n < 0;
    if (negative) {
      if (n == Long.MIN_VALUE) {
        dst[offset + (--len)] = toDigitLowerCaseUnchecked((int) -(n % base));
        n /= base;
      }
      n = -n;
    }

    int i;
    for (i = offset + len - 1; i >= offset && n > Integer.MAX_VALUE; i--, n /= base)
      dst[i] = toDigitLowerCaseUnchecked((int)(n % base));

    int intN = (int) n, intBase = (int) base;
    for (; i >= offset && intN != 0; i--, intN /= intBase)
      dst[i] = toDigitLowerCaseUnchecked(intN % intBase);

    if (i >= offset) {
      if (negative)
        dst[offset++] = '-';
      java.util.Arrays.fill(dst, offset, i + 1, '0');
    }

    return dst;
  }


  public static boolean isAscii( int c )
  {
    return c >>> 7 == 0;
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
    return s.length() == (long) prefix.length() + suffix.length() &&
      s.startsWith(prefix) && s.endsWith(suffix);
  }


  public static final Pattern SIMPLE_URL_PREFIX_PATTERN =
    Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE);

  public static boolean looksLikeUrl( CharSequence s )
  {
    return SIMPLE_URL_PREFIX_PATTERN.matcher(s).find();
  }


  public static int countLeadingWhitespace( CharSequence csq, int offset )
  {
    final int len = csq.length();
    while (offset < len)
    {
      int c = Character.codePointAt(csq, offset);
      offset += Character.charCount(c);
      if (Character.isWhitespace(c))
        break;
    }
    return offset;
  }


  @FunctionalInterface
  public interface ReplaceCallback
  {
    enum Replacement
    {
      /**
       * Replace the current match with itself.
       */
      @SuppressWarnings("SpellCheckingInspection")
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

    int lastMatchEnd = 0, inputEnd = input.length();
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

        case STOP:
          inputEnd = matcher.start();
          break matcherLoop;

        case ABORT:
          return null;
        }
      }

      if (buf == null)
        buf = new StringBuilder(matcher.start() + 16);

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

    return (buf != null && buf.length() != 0) ?
      buf.append(input, lastMatchEnd, inputEnd) :
      subSequence(input, lastMatchEnd, inputEnd);
  }


  private static final Pattern CAMEL_CASE_CONVERSION_PATTERN =
    Pattern.compile("[\\s-]+(\\p{javaLowerCase}?)");


  public static CharSequence toCamelCase( CharSequence s )
  {
    return
      (s.length() > 1) ?
        replaceAll(CAMEL_CASE_CONVERSION_PATTERN, s,
          Strings::toCamelCaseReplaceCallback) :
        s;
  }


  private static Object toCamelCaseReplaceCallback( MatchResult matcher,
    CharSequence input, @SuppressWarnings("unused") StringBuilder sb )
  {
    int start1 = matcher.start(1), end1 = matcher.end(1);
    char[] cs;
    if (start1 == end1)
    {
      cs = ArrayUtils.EMPTY_CHAR_ARRAY;
    }
    else
    {
      int c = Character.codePointAt(input, start1);
      assert start1 + Character.charCount(c) == end1;
      cs = Character.toChars(
        (matcher.start() != 0) ? Character.toUpperCase(c) : c);
    }
    return cs;
  }


  public static boolean isInteger( CharSequence s )
  {
    final int len = s.length();
    if (len == 0)
      return false;

    int c = Character.codePointAt(s, 0);
    int i = Character.charCount(c);
    if (!Character.isDigit(c) && (len == i || (c != '+' && c != '-')))
      return false;

    for (; i < len; i += Character.charCount(c))
    {
      c = Character.codePointAt(s, i);
      if (!Character.isDigit(c))
        return false;
    }
    return true;
  }


  public static boolean endsWith( String haystack, String needle,
    boolean ignoreCase )
  {
    return haystack.regionMatches(ignoreCase,
      haystack.length() - needle.length(), needle, 0, needle.length());
  }


  public static CharSequence subSequence( CharSequence csq, int start,
    int end )
  {
    return
      (start == end) ? "" :
      (start == 0 && end == csq.length()) ? csq :
        csq.subSequence(start, end);
  }


  public static CharSequence join( CharSequence a, CharSequence b,
    char delimiter )
  {
    int aLen = a.length(), bLen = b.length();
    if (aLen == 0)
      return b;
    if (bLen == 0)
      return a;

    char[] result = new char[aLen + bLen + 1];
    getChars(a, 0, aLen, result, 0);
    result[aLen] = delimiter;
    getChars(b, 0, bLen, result, aLen + 1);
    return new String(result);
  }


  public static void getChars( CharSequence src, int srcBegin, int srcEnd,
    char[] dst, int dstOffset )
  {
    if (src instanceof String)
    {
      ((String) src).getChars(srcBegin, srcEnd, dst, dstOffset);
    }
    else if (src instanceof StringBuilder)
    {
      ((StringBuilder) src).getChars(srcBegin, srcEnd, dst, dstOffset);
    }
    else if (src instanceof StringBuffer)
    {
      ((StringBuffer) src).getChars(srcBegin, srcEnd, dst, dstOffset);
    }
    else if (src instanceof CharBuffer)
    {
      CharBuffer cb = (CharBuffer) src;
      int mark = cb.position();
      cb.position(mark + srcBegin);
      try {
        cb.get(dst, dstOffset, srcEnd - srcBegin);
      } finally {
        cb.position(mark);
      }
    }
    else
    {
      for (; srcBegin < srcEnd; srcBegin++, dstOffset++)
        dst[dstOffset] = src.charAt(srcBegin);
    }
  }


  public static char[] getChars( CharSequence csq )
  {
    char[] a = new char[csq.length()];
    getChars(csq, 0, a.length, a, 0);
    return a;
  }
}
