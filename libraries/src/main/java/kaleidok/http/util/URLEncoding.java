package kaleidok.http.util;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.BitSet;

import static java.lang.System.arraycopy;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.CodingErrorAction.REPORT;
import static kaleidok.util.AssertionUtils.fastAssert;
import static org.apache.commons.lang3.StringEscapeUtils.ESCAPE_JAVA;


public final class URLEncoding
{
  private URLEncoding() { }

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final int LOWERCASE_BIT = 'A' ^ 'a';

  public static final char
    ESCAPE_PREFIX = '%',
    SPACE_ESCAPER = '+';

  /**
   * The list of characters that are not encoded has been determined as
   * follows:
   *
   * RFC 2396 states:
   * -----
   * Data characters that are allowed in a URI but do not have a reserved
   * purpose are called unreserved.  These include upper and lower case
   * letters, decimal digits, and a limited set of punctuation marks and
   * symbols.
   *
   * unreserved  = alphanum | mark
   *
   * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
   *
   * Unreserved characters can be escaped without changing the semantics
   * of the URI, but this should not be done unless the URI is being used in
   * a context that does not allow the unescaped character to appear.
   * -----
   *
   * It appears that both Netscape and Internet Explorer escape all special
   * characters from this list with the exception of "-", "_", ".", "*".
   * While it is not clear why they are escaping the other characters,
   * perhaps it is safest to assume that there might be contexts in which the
   * others are unsafe if not escaped. Therefore, we will use the same list.
   * It is also noteworthy that this is consistent with O'Reilly's "HTML: The
   * Definitive Guide" (page 164).
   *
   * As a last note, Internet Explorer does not encode the "@" character
   * which is clearly not unreserved according to the RFC. We are being
   * consistent with the RFC in this matter, as is Netscape.
   */
  static final BitSet dontNeedEncoding =
    BitSet.valueOf(new long[]{ 0x3ff640100000000L, 0x7fffffe87fffffeL });


  public static String encode( String s )
  {
    return encode(s, DEFAULT_CHARSET);
  }

  public static StringBuilder appendEncoded( CharSequence s, StringBuilder dst )
  {
    encode(s, DEFAULT_CHARSET, dst);
    return dst;
  }

  public static String encode( String s, Charset charset )
  {
    return encode((CharSequence) s, charset).toString();
  }

  public static CharSequence encode( CharSequence s )
  {
    return encode(s, DEFAULT_CHARSET);
  }

  /**
   * Translates a string into {@code application/x-www-form-urlencoded} format
   * using a specific encoding scheme. This method uses the supplied encoding
   * scheme to obtain the bytes for unsafe characters.
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that UTF-8 should be
   * used. Not doing so may introduce incompatibilities.</em>
   *
   * @param   s   {@code String} to be translated.
   * @param   charset   The name of a supported character encoding.
   * @return  the translated {@code CharSequence}.
   * @see #decode(CharSequence, Charset)
   */
  public static CharSequence encode( CharSequence s, Charset charset )
  {
    return encode(s, charset, null);
  }

  public static CharSequence encode( CharSequence _s, Charset charset, StringBuilder dst )
  {
    if (_s.length() == 0)
      return (dst != null) ? dst : _s;

    CharBuffer s;
    if (_s instanceof CharBuffer) {
      s = (CharBuffer) _s;
      if (s.position() != 0 || s.limit() != s.capacity())
        s = s.slice();
    } else {
      s = CharBuffer.wrap(_s);
    }

    CharsetEncoder enc = null;
    ByteBuffer bytes = null;

    int currentRunOffset = 0;
    while (s.hasRemaining())
    {
      char c = s.get();

      if (c != ' ' && dontNeedEncoding.get(c))
        continue;

      if (dst == null) {
        dst = new StringBuilder(
          s.position() - 1 + estimateEncoderOutputLength(s.remaining() + 1));
      }
      appendTo(dst, _s, currentRunOffset, s.position() - 1);

      if (c == ' ')
      {
        dst.append(SPACE_ESCAPER);
      }
      else
      {
        if (enc == null)
        {
          enc = charset.newEncoder()
            .onMalformedInput(REPORT)
            .onUnmappableCharacter(REPLACE);

          int remaining = s.remaining() + 1;
          dst.ensureCapacity(
            dst.length() + estimateEncoderOutputLength(remaining, 1, enc));

          bytes = ByteBuffer.allocate(
            (int) enc.maxBytesPerChar() *
              Math.min(CODER_MAX_CHARS_CAPACITY, remaining));
        }
        else
        {
          enc.reset();
        }

        currentRunOffset = s.position() - 1;
        while (s.hasRemaining())
        {
          c = s.get();
          if (dontNeedEncoding.get(c))
          {
            s.limit(s.position() - 1);
            break;
          }
        }
        s.position(currentRunOffset);

        boolean isEndOfInput = s.limit() == s.capacity();
        CoderResult cr;
        do
        {
          cr = enc.encode(s, bytes, isEndOfInput);
          if (isEndOfInput && cr.isUnderflow())
            cr = enc.flush(bytes);
          if (cr.isError())
          {
            throw new IllegalArgumentException(
              cr.isMalformed() ? new MalformedInputException(cr.length()) :
              cr.isUnmappable() ? new UnmappableCharacterException(cr.length()) :
                new CharacterCodingException());
          }

          bytes.limit(bytes.position());
          bytes.rewind();
          while (bytes.hasRemaining())
          {
            int b = bytes.get() & 0xff;
            dst.append(ESCAPE_PREFIX)
              .append((char) toHexDigit(b >>> 4))
              .append((char) toHexDigit(b & 0xf));
          }
          bytes.limit(bytes.capacity());
          bytes.rewind();
        }
        while (cr.isOverflow());

        fastAssert(!s.hasRemaining());
        s.limit(s.capacity());
      }

      currentRunOffset = s.position();
    }


    return (dst == null) ? _s : appendTo(dst, _s, currentRunOffset, _s.length());
  }


  private static final int CODER_MAX_CHARS_CAPACITY = 8;

  private static int toHexDigit( int c )
  {
    assert c >>> 4 == 0 :
      String.format("%#010x (%<d) is outside of [0, 16)", c);

    return c + ((c < 10) ? '0' : ('A' - 10));
  }


  private static int estimateEncoderOutputLength( int inputLength )
  {
    return inputLength + inputLength / 2;
  }

  private static int estimateEncoderOutputLength( int inputLength,
    int needEncodingCount, CharsetEncoder enc )
  {
    // Assumption: up to 1/4 of input characters need to be encoded
    assert inputLength >= needEncodingCount :
      inputLength + " < " + needEncodingCount;

    inputLength -= needEncodingCount;
    return 1 + (int)(
      inputLength * 0.75f * (1f + enc.averageBytesPerChar()) +
      needEncodingCount * 3 * enc.averageBytesPerChar());
  }


  public static String decode( String s )
  {
    return decode((CharSequence) s).toString();
  }

  public static CharSequence decode( CharSequence s )
  {
    return decode(s, DEFAULT_CHARSET);
  }

  public static String decode( String s, Charset charset )
  {
    return decode((CharSequence) s, charset).toString();
  }

  /**
   * Decodes a {@code application/x-www-form-urlencoded} string using a
   * specific encoding scheme. The supplied encoding is used to determine what
   * characters are represented by any consecutive sequences of the form
   * "<code>%<i>xy</i></code>".
   * <p>
   * <em><strong>Note:</strong> The <a href=
   * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
   * World Wide Web Consortium Recommendation</a> states that UTF-8 should be
   * used. Not doing so may introduce incompatibilities.</em>
   *
   * @param s the {@code CharSequence} to decode
   * @param charset   A supported character encoding.
   * @return the newly decoded {@code CharSequence}
   * @see #encode(CharSequence, Charset)
   */
  public static CharSequence decode( CharSequence s, Charset charset )
  {
    return decode(s, charset, null);
  }

  public static CharSequence decode( CharSequence s, Charset charset, StringBuilder dst )
  {
    CharsetDecoder dec = null;
    byte[] aBytes = null;
    ByteBuffer bytes = null;
    char[] aChars = null;
    CharBuffer chars = null;

    final int len = s.length();
    int i = 0, currentRunOffset = 0;
    while (i < len)
    {
      char c = s.charAt(i++);
      if (c != SPACE_ESCAPER && c != ESCAPE_PREFIX)
        continue;

      if (dst == null)
        dst = new StringBuilder(estimateDecoderOutputLength(s, i));
      appendTo(dst, s, currentRunOffset, i - 1);

      if (c == SPACE_ESCAPER)
      {
        dst.append(' ');
      }
      else
      {
        if (dec == null)
        {
          dec = charset.newDecoder()
            .onMalformedInput(REPORT)
            .onUnmappableCharacter(REPLACE);

          aBytes = new byte[CODER_MAX_CHARS_CAPACITY * 2];
          bytes = ByteBuffer.wrap(aBytes);

          aChars = new char[(int)(bytes.capacity() * dec.maxCharsPerByte())];
          chars = CharBuffer.wrap(aChars);
        }
        else
        {
          dec.reset();
        }

        /*
         * Starting with this instance of %, process all consecutive substrings
         * of the form %xy. Each substring %xy will yield a byte. Convert all
         * consecutive bytes obtained this way to whatever character(s) they
         * represent in the provided encoding.
         */
        boolean reachedEndOfSequence = false;
        do
        {
          while (i + 1 < len && bytes.hasRemaining())
          {
            int v = hexDigitValue(s.charAt(i++));
            v = (v << 4) | hexDigitValue(s.charAt(i++));
            if ((v & ~0xff) != 0)
            {
              throw new IllegalArgumentException(String.format(
                "Illegal hex characters in escape pattern: \"%c%s\"",
                ESCAPE_PREFIX, ESCAPE_JAVA.translate(s.subSequence(i - 2, i))));
            }
            bytes.put((byte) v);

            if (i >= len || s.charAt(i) != ESCAPE_PREFIX) {
              reachedEndOfSequence = true;
              break;
            }
            i++;
          }

          if (bytes.position() == 0) {
            fastAssert(bytes.hasRemaining(), "empty byte buffer");
            break;
          }

          bytes.limit(bytes.position());
          bytes.rewind();
          CoderResult cr = dec.decode(bytes, chars, reachedEndOfSequence);
          if (reachedEndOfSequence && cr.isUnderflow())
            cr = dec.flush(chars);
          if (!cr.isUnderflow())
          {
            if (cr.isOverflow()) {
              throw new AssertionError(new BufferOverflowException());
            }
            if (cr.isMalformed()) {
              throw new IllegalArgumentException(
                new MalformedInputException(cr.length()));
            }
            assert cr.isUnmappable() :
              cr + " is expected to have an \"unmappable\" condition";
            byte[] unmappable = new byte[cr.length()];
            bytes.get(unmappable);
            throw new IllegalArgumentException(
              Arrays.toString(unmappable) + " are unmappable from " +
                charset.name(),
              new UnmappableCharacterException(cr.length()));
          }

          fastAssert(chars.arrayOffset() == 0);
          dst.append(aChars, 0, chars.position());
          chars.rewind();

          fastAssert(bytes.arrayOffset() == 0);
          int remaining = bytes.remaining();
          arraycopy(aBytes, bytes.position(), aBytes, 0, remaining);
          bytes.position(remaining);
          bytes.limit(bytes.capacity());
        }
        while (!reachedEndOfSequence);

        /*
         * A trailing, incomplete byte encoding such as "%x" will cause an
         * exception to be thrown.
         */
        if (!reachedEndOfSequence) {
          throw new IllegalArgumentException(
            "Incomplete trailing escape (%) pattern");
        }
      }

      currentRunOffset = i;
    }

    return (dst == null) ? s : appendTo(dst, s, currentRunOffset, len);
  }


  private static int hexDigitValue( int c )
  {
    if (c >= '0' && c <= '9')
      return c - '0';
    c &= ~LOWERCASE_BIT; // fast upper case
    return (c >= 'A' && c <= 'F') ? c - ('A' - 10) : -1;
  }


  private static int countEscapeSequences( CharSequence s, int begin, final int end )
  {
    assert 0 <= begin && begin <= end && end <= s.length() :
      String.format("0 ≤ %d ≤ %d ≤ %d doesn’t hold", begin, end, s.length());
    int escapedCount = 0;
    while (begin < end) {
      if (s.charAt(begin++) == ESCAPE_PREFIX) {
        begin += 2;
        escapedCount++;
      }
    }
    return escapedCount;
  }


  private static int estimateDecoderOutputLength( CharSequence s, int offset )
  {
    return s.length() - countEscapeSequences(s, offset, s.length()) * 2;
  }


  private static StringBuilder appendTo( StringBuilder a, CharSequence csq,
    int start, int end )
  {
    if (csq.length() > 1 && csq instanceof CharBuffer) {
      CharBuffer cb = (CharBuffer) csq;
      if (cb.hasArray()) {
        return a.append(cb.array(), start + cb.arrayOffset() + cb.position(), end - start);
      }
    }
    return a.append(csq, start, end);
  }
}
