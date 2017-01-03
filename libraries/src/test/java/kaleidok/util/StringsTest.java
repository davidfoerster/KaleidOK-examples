package kaleidok.util;

import org.junit.Test;

import static kaleidok.util.Strings.toHexDigits;
import static org.junit.Assert.*;


public class StringsTest
{
  @Test
  public void testToHexString1()
  {
    char[] buf = new char[0];
    assertSame(buf, toHexDigits(0, buf));
    assertSame(buf, toHexDigits(Long.MIN_VALUE, buf));
    assertSame(buf, toHexDigits(Long.MAX_VALUE, buf));
  }

  @Test
  public void testToHexString2()
  {
    assertEquals('0', toHexDigits(0, new char[1])[0]);
    assertEquals('0', toHexDigits(Long.MIN_VALUE, new char[1])[0]);
    assertEquals('f', toHexDigits(Long.MAX_VALUE, new char[1])[0]);
    assertEquals('4', toHexDigits(0x1234, new char[1])[0]);
  }

  @Test
  public void testToHexString3()
  {
    assertEquals("0000000000000000", new String(toHexDigits(0, null)));
    assertEquals("8000000000000000", new String(toHexDigits(Long.MIN_VALUE, null)));
    assertEquals("7fffffffffffffff", new String(toHexDigits(Long.MAX_VALUE, null)));
    assertEquals("0000000000001234", new String(toHexDigits(0x1234, null)));
  }

  @Test
  public void testToHexString4()
  {
    assertEquals("000000", new String(toHexDigits(0, new char[6])));
    assertEquals("000000", new String(toHexDigits(Long.MIN_VALUE, new char[6])));
    assertEquals("ffffff", new String(toHexDigits(Long.MAX_VALUE, new char[6])));
    assertEquals("001234", new String(toHexDigits(0x1234, new char[6])));
  }

  @Test
  public void testToHexString5()
  {
    assertEquals("00000000000000000", new String(toHexDigits(0, new char[17])));
    assertEquals("08000000000000000", new String(toHexDigits(Long.MIN_VALUE, new char[17])));
    assertEquals("07fffffffffffffff", new String(toHexDigits(Long.MAX_VALUE, new char[17])));
    assertEquals("00000000000001234", new String(toHexDigits(0x1234, new char[17])));
  }
}
