package kaleidok.chromatik;

import org.junit.Test;

import static org.junit.Assert.*;


public class UtilsTest
{
  @Test
  public void testToHexString1() throws Exception
  {
    char[] buf = new char[0];
    assertSame(buf, Utils.toHex(0, buf));
    assertSame(buf, Utils.toHex(Long.MIN_VALUE, buf));
    assertSame(buf, Utils.toHex(Long.MAX_VALUE, buf));
  }

  @Test
  public void testToHexString2() throws Exception
  {
    assertEquals('0', Utils.toHex(0, new char[1])[0]);
    assertEquals('0', Utils.toHex(Long.MIN_VALUE, new char[1])[0]);
    assertEquals('f', Utils.toHex(Long.MAX_VALUE, new char[1])[0]);
    assertEquals('4', Utils.toHex(0x1234, new char[1])[0]);
  }

  @Test
  public void testToHexString3() throws Exception
  {
    assertEquals("0000000000000000", new String(Utils.toHex(0, null)));
    assertEquals("8000000000000000", new String(Utils.toHex(Long.MIN_VALUE, null)));
    assertEquals("7fffffffffffffff", new String(Utils.toHex(Long.MAX_VALUE, null)));
    assertEquals("0000000000001234", new String(Utils.toHex(0x1234, null)));
  }

  @Test
  public void testToHexString4() throws Exception
  {
    assertEquals("000000", new String(Utils.toHex(0, new char[6])));
    assertEquals("000000", new String(Utils.toHex(Long.MIN_VALUE, new char[6])));
    assertEquals("ffffff", new String(Utils.toHex(Long.MAX_VALUE, new char[6])));
    assertEquals("001234", new String(Utils.toHex(0x1234, new char[6])));
  }

  @Test
  public void testToHexString5() throws Exception
  {
    assertEquals("00000000000000000", new String(Utils.toHex(0, new char[17])));
    assertEquals("08000000000000000", new String(Utils.toHex(Long.MIN_VALUE, new char[17])));
    assertEquals("07fffffffffffffff", new String(Utils.toHex(Long.MAX_VALUE, new char[17])));
    assertEquals("00000000000001234", new String(Utils.toHex(0x1234, new char[17])));
  }
}
