package kaleidok.http.util;

import kaleidok.util.Strings;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import static java.lang.Character.*;
import static java.nio.charset.StandardCharsets.*;
import static kaleidok.http.util.URLEncoding.decode;
import static kaleidok.http.util.URLEncoding.encode;
import static org.junit.Assert.*;


public class URLEncodingTest
{
  private static final int
    RANDOM_TEST_COUNT = 10,
    RANDOM_TEST_LENGTH = 40;

  private static final Collection<Charset> CHARSETS =
    new ArrayList<Charset>(10) {{
      add(UTF_8);
      add(UTF_16BE);
      add(Charset.forName("UTF-32BE"));
      add(ISO_8859_1);
      add(Charset.forName("ISO-8859-15"));

      for (int i = 1250; i <= 1252; i ++)
      {
        //noinspection InjectedReferences
        add(Charset.forName("Windows-" + i));
      }

      Charset defaultPlatformCharset = Charset.defaultCharset();
      if (!contains(defaultPlatformCharset))
        add(defaultPlatformCharset);
    }};


  @Test
  public void testDecode1()
  {
    String s = "";
    for (Charset chs: CHARSETS)
      assertSame(s, decode(s, chs));
  }

  @Test
  public void testEncode1()
  {
    String s = "";
    for (Charset chs: CHARSETS)
      assertSame(s, encode(s, chs));
  }

  @Test
  public void testDecode2()
  {
    String s = "abc";
    for (Charset chs: CHARSETS)
      assertSame(s, decode(s, chs));
  }

  @Test
  public void testEncode2()
  {
    String s = "abc";
    for (Charset chs: CHARSETS)
      assertSame(s, encode(s, chs));
  }

  private static final String SPACE_ESCAPER =
    Character.toString(URLEncoding.SPACE_ESCAPER);

  @Test
  public void testDecode3()
  {
    for (Charset chs: CHARSETS)
      assertEquals(" ", decode(SPACE_ESCAPER, chs));
  }

  @Test
  public void testEncode3()
  {
    for (Charset chs: CHARSETS)
      assertEquals(SPACE_ESCAPER, encode(" ", chs));
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testDecode4() throws UnsupportedEncodingException
  {
    testDecode("&");
    testDecode("%");
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testEncode4() throws UnsupportedEncodingException
  {
    testEncode("&");
    testEncode("%");
  }

  @SuppressWarnings({ "JUnitTestMethodWithNoAssertions", "SpellCheckingInspection" })
  @Test
  public void testDecode5() throws UnsupportedEncodingException
  {
    testDecode("Föøβäя +&% bàç!?");
  }

  @SuppressWarnings({ "JUnitTestMethodWithNoAssertions", "SpellCheckingInspection" })
  @Test
  public void testEncode5() throws UnsupportedEncodingException
  {
    testEncode("Föøβäя +&% bàç!?");
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testDecode6()
  {
    decodeExpectIllegalArgumentException("%", UTF_8);
    decodeExpectIllegalArgumentException("%2", UTF_8);
    decodeExpectIllegalArgumentException("%G0", UTF_8);
    decodeExpectIllegalArgumentException("%2G", UTF_8);
    decodeExpectIllegalArgumentException("%20%", UTF_8);
    decodeExpectIllegalArgumentException("%20%2", UTF_8);
    decodeExpectIllegalArgumentException("%20-%", UTF_8);
    decodeExpectIllegalArgumentException("%20-%2", UTF_8);
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testEncode6()
  {
    assert isValidCodePoint(MAX_CODE_POINT);

    String surrogatePair = new String(toChars(MAX_CODE_POINT));
    assert surrogatePair.length() == 2;

    // Disabled, since unmappable characters should be replaced, not reported
    /*
    encodeExpectIllegalArgumentException("…",
      ISO_8859_1, UnmappableCharacterException.class);
    encodeExpectIllegalArgumentException(surrogatePair,
      ISO_8859_1, UnmappableCharacterException.class);
    */

    encodeExpectIllegalArgumentException(surrogatePair.substring(0, 1),
      UTF_8, MalformedInputException.class);
    encodeExpectIllegalArgumentException(surrogatePair.substring(1, 2),
      UTF_8, MalformedInputException.class);
    encodeExpectIllegalArgumentException(
      surrogatePair.substring(1, 2) + surrogatePair.charAt(0),
      UTF_8, MalformedInputException.class);
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testDecode7()
    throws UnsupportedEncodingException, MalformedInputException
  {
    testRandom(true);
  }

  @SuppressWarnings("JUnitTestMethodWithNoAssertions")
  @Test
  public void testEncode7()
    throws UnsupportedEncodingException, MalformedInputException
  {
    testRandom(false);
  }


  private static void testRandom( boolean decode )
    throws UnsupportedEncodingException, MalformedInputException
  {
    StringBuilder sb = new StringBuilder(RANDOM_TEST_LENGTH);
    for (int i = 0; i < RANDOM_TEST_COUNT; i++)
    {
      for (Charset chs: CHARSETS)
      {
        String unicodeStr;
        if (chs.contains(UTF_8))
        {
          sb.setLength(0);
          appendRandom(sb, MIN_CODE_POINT, MAX_CODE_POINT, 1, sb.capacity() / 2);
          unicodeStr = sb.toString();
        }
        else
        {
          ByteBuffer bytes = ByteBuffer.allocate(getRandom(1, RANDOM_TEST_LENGTH));
          rand.nextBytes(bytes.array());
          unicodeStr = ignoreUnmappable(bytes, chs);
        }

        test(decode, unicodeStr, chs);
      }
    }
  }

  private static void test( boolean decode, String s, Charset chs )
    throws UnsupportedEncodingException
  {
    if (decode) {
      testDecode(s, chs);
    } else {
      testEncode(s, chs);
    }
  }

  private static void testDecode( String s )
    throws UnsupportedEncodingException
  {
    for (Charset chs: CHARSETS)
      testDecode(replaceUnmappable(s, chs), chs);
  }

  private static void testDecode( String s, Charset chs )
    throws UnsupportedEncodingException
  {
    String urlEncoded = URLEncoder.encode(s, chs.name());
    assertEquals(
      replaceUnmappable(s, chs),
      decode(urlEncoded, chs));
  }


  private static void decodeExpectIllegalArgumentException( String s, Charset chs )
  {
    expectIllegalArgumentException(true, s, chs, null);
  }


  private static void testEncode( String s )
    throws UnsupportedEncodingException
  {
    for (Charset chs: CHARSETS)
      testEncode(replaceUnmappable(s, chs), chs);
  }

  private static void testEncode( String s, Charset chs )
    throws UnsupportedEncodingException
  {
    assertEquals(
      URLEncoder.encode(s, chs.name()),
      encode(s, chs));
  }

  private static void encodeExpectIllegalArgumentException( String s,
    Charset chs, Class<? extends Throwable> expectedCause )
  {
    expectIllegalArgumentException(false, s, chs, expectedCause);
  }


  private static void expectIllegalArgumentException( boolean decode, String s,
    Charset chs, Class<? extends Throwable> expectedCause )
  {
    String result;
    try {
      result = decode ? decode(s, chs) : encode(s, chs);
      if (expectedCause == null)
        expectedCause = IllegalArgumentException.class;
    } catch (IllegalArgumentException ex) {
      Throwable cause = ex.getCause();
      if (expectedCause == null || (cause != null &&
        expectedCause.isAssignableFrom(cause.getClass())))
      {
        return;
      }

      StringWriter writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer, false);
      ex.printStackTrace(printWriter);
      printWriter.close();
      result = writer.toString();
    }

    fail(String.format(
      "Expected an %s when %scoding \"%s\"; instead I got:%n%s",
      expectedCause.getCanonicalName(), decode ? "de" : "en", s, result));
  }

  private static String replaceUnmappable( String s, Charset chs )
  {
    return (chs.contains(UTF_8) || (chs.contains(US_ASCII) && Strings.isAscii(s)) ?
      s :
      new String(s.getBytes(chs), chs));
  }

  private static String ignoreUnmappable( ByteBuffer bytes, Charset chs )
    throws MalformedInputException
  {
    int p = bytes.position();
    CharsetDecoder dec = chs.newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.IGNORE);

    String result;
    try {
       result = dec.decode(bytes).toString();
    } catch (MalformedInputException ex) {
      throw ex;
    } catch (CharacterCodingException ex) {
      throw new AssertionError(ex);
    }

    bytes.position(p);
    return result;
  }


  private static final Random rand = new Random();

  private static void appendRandom( StringBuilder buf,
    int minChar, int maxChar, int minCount, int maxCount )
  {
    appendRandom(buf, minChar, maxChar, getRandom(minCount, maxCount));
  }

  private static void appendRandom( StringBuilder buf,
    int minChar, int maxChar, int count )
  {
    for (; count > 0; count--) {
      int c = getRandomCodePoint(minChar, maxChar);
      if (!isSupplementaryCodePoint(c)) {
        buf.append((char) c);
      } else {
        buf.append(highSurrogate(c)).append(lowSurrogate(c));
      }
    }
  }

  private static int getRandomCodePoint( int minChar, int maxChar )
  {
    assert isValidCodePoint(minChar) && isValidCodePoint(maxChar) :
      String.format("Contains invalid code points: %d, %d", minChar, maxChar);

    while (true) {
      int c = getRandom(minChar, maxChar);
      if (!isValidCodePoint(c) || isISOControl(c))
        continue;
      if (c <= MAX_VALUE && isSurrogate((char) c))
        continue;
      UnicodeBlock block = UnicodeBlock.of(c);
      if (block == null || block == UnicodeBlock.SPECIALS || block == UnicodeBlock.PRIVATE_USE_AREA)
        continue;
      return c;
    }
  }

  private static int getRandom( int min, int max )
  {
    assert min <= max : min + " > " + max;
    //noinspection UnnecessaryExplicitNumericCast
    assert (long) max - (long) min + 1 <= Integer.MAX_VALUE :
      String.format("(%d - %d + 1) overflows", max, min);

    return rand.nextInt(max - min + 1) + min;
  }
}
