package kaleidok.text;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static kaleidok.test.matcher.BigDecimalComparesEqual.comparesEqual;
import static kaleidok.text.InternationalSystemOfUnitsFormat.*;
import static kaleidok.util.Math.clamp;
import static org.junit.Assert.*;


public class InternationalSystemOfUnitsFormatTest
{
  //@Rule
  //public final ErrorCollector errorCollector = new ErrorCollector();

  private static final String UNIT = "Hz";

  private InternationalSystemOfUnitsFormat fmt;


  @Before
  public void setUp()
  {
    fmt = new InternationalSystemOfUnitsFormat(
      (DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT),
      ' ' + UNIT);
    fmt.setGroupingUsed(false);
    fmt.enforceUnitWhenParsing = false;
  }


  @Test
  public void testFormatLong1()
  {
    assertEquals("0 Hz", fmt.format(0));
    assertEquals("1 Hz", fmt.format(1));
    assertEquals("-1 Hz", fmt.format(-1));
    assertEquals("9 Hz", fmt.format(9));
    assertEquals("-9 Hz", fmt.format(-9));
    assertEquals("99 Hz", fmt.format(99));
    assertEquals("-99 Hz", fmt.format(-99));
    assertEquals("999 Hz", fmt.format(999));
    assertEquals("-999 Hz", fmt.format(-999));
  }


  @Test
  public void testFormatLong2()
  {
    StringBuilder expected = new StringBuilder();

    for (int log10 = 0; log10 <= 18; log10++)
    {
      int magnitude = log10 / 3;
      long n = BigInteger.TEN.pow(log10).longValueExact();
      long nScaled = n / BigInteger.TEN.pow(magnitude * 3).longValueExact();

      expected.setLength(0);
      expected.append('-').append(nScaled).append(' ');
      if (magnitude != 0)
        expected.append(MAGNITUDE_CHAR_MAP.get(magnitude).charValue());
      expected.append(UNIT);
      String result1 = fmt.format(n);
      assertEquals(expected.substring(1), result1);

      String result2 = fmt.format(-n);
      assertEquals(expected.toString(), result2);
    }
  }


  @Test
  public void testFormatLong3()
  {
    assertEquals("1 kHz", fmt.format(1000));
    assertEquals("-1 kHz", fmt.format(-1000));
    assertEquals("1.1 kHz", fmt.format(1100));
    assertEquals("-1.1 kHz", fmt.format(-1100));
    assertEquals("9.999 kHz", fmt.format(9999));
    assertEquals("-9.999 kHz", fmt.format(-9999));
  }


  @Test
  public void testFormatDouble1()
  {
    assertEquals("1 Hz", fmt.format(1d));
    assertEquals("-1 Hz", fmt.format(-1d));
    assertEquals("1 kHz", fmt.format(1e3));
    assertEquals("1.1 kHz", fmt.format(1.1e3));
    assertEquals("999.999 kHz", fmt.format(999999d));
    assertEquals("10.01 μHz", fmt.format(10.01e-6));
  }


  @Test
  public void testFormatDouble2()
  {
    DecimalFormatSymbols sym = fmt.getDecimalFormatSymbols();
    assertEquals(sym.getNaN() + ' ' + UNIT, fmt.format(Double.NaN));
    assertEquals(sym.getInfinity() + ' ' + UNIT,
      fmt.format(Double.POSITIVE_INFINITY));
    assertEquals('-' + sym.getInfinity() + ' ' + UNIT,
      fmt.format(Double.NEGATIVE_INFINITY));
  }


  @Test
  public void testFormatBigInteger1()
  {
    assertEquals("0 Hz", fmt.format(BigInteger.ZERO));
    assertEquals("1 Hz", fmt.format(BigInteger.ONE));
    assertEquals("-1 Hz", fmt.format(BigInteger.valueOf(-1)));
    assertEquals("1 kHz", fmt.format(BigInteger.valueOf(1000)));
    assertEquals("1.1 kHz", fmt.format(BigInteger.valueOf(1100)));
    assertEquals("999.999 kHz", fmt.format(BigInteger.valueOf(999999)));
    assertEquals("42.42 ZHz",
      fmt.format(BigInteger.valueOf(4242).multiply(BigInteger.TEN.pow(19))));
    assertEquals("1000 YHz", fmt.format(BigInteger.TEN.pow(27)));
  }


  @Test
  public void testFormatBigInteger2()
  {
    StringBuilder expected = new StringBuilder();

    for (int log10 = 0; log10 <= 27; log10++)
    {
      int magnitude = clamp(log10 / 3, MAGNITUDE_MIN, MAGNITUDE_MAX);
      BigInteger n = BigInteger.TEN.pow(log10);
      BigInteger nScaled = BigInteger.TEN.pow(log10 - magnitude * 3);

      expected.setLength(0);
      expected.append('-').append(nScaled).append(' ');
      if (magnitude != 0)
        expected.append(MAGNITUDE_CHAR_MAP.get(magnitude).charValue());
      expected.append(UNIT);
      String result1 = fmt.format(n);
      assertEquals(expected.substring(1), result1);

      String result2 = fmt.format(n.negate());
      assertEquals(expected.toString(), result2);
    }
  }


  @Test
  public void testGetMagnitudeBigDecimal1()
  {
    assertEquals(0, fmt.getMagnitude(BigDecimal.ZERO));
    assertEquals(0, fmt.getMagnitude(BigDecimal.ONE));
    assertEquals(0, fmt.getMagnitude(BigDecimal.TEN));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(100)));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(999)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(1000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(10000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(100000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(999999)));
    assertEquals(2, fmt.getMagnitude(BigDecimal.valueOf(1000000)));

    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(-1)));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(-10)));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(-100)));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(-999)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(-1000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(-10000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(-100000)));
    assertEquals(1, fmt.getMagnitude(BigDecimal.valueOf(-999999)));
    assertEquals(2, fmt.getMagnitude(BigDecimal.valueOf(-1000000)));
  }


  @Test
  public void testGetMagnitudeBigDecimal2()
  {
    assertEquals(-1, fmt.getMagnitude(BigDecimal.valueOf(1e-1)));
    assertEquals(-1, fmt.getMagnitude(BigDecimal.valueOf(1e-2)));
    assertEquals(-1, fmt.getMagnitude(BigDecimal.valueOf(1e-3)));
    assertEquals(-1, fmt.getMagnitude(BigDecimal.valueOf(999e-3)));
    assertEquals(-2, fmt.getMagnitude(BigDecimal.valueOf(1e-4)));
    assertEquals(-2, fmt.getMagnitude(BigDecimal.valueOf(1e-5)));
    assertEquals(-2, fmt.getMagnitude(BigDecimal.valueOf(1e-6)));
    assertEquals(-2, fmt.getMagnitude(BigDecimal.valueOf(999e-6)));
    assertEquals(-3, fmt.getMagnitude(BigDecimal.valueOf(1e-7)));

    assertEquals(-1, fmt.getMagnitude(BigDecimal.valueOf(999.999e-3)));
    assertEquals(0, fmt.getMagnitude(BigDecimal.valueOf(1.999999)));
  }


  @Test
  public void testFormatBigDecimal1()
  {
    assertEquals("0 Hz", fmt.format(BigDecimal.ZERO));
    assertEquals("1 Hz", fmt.format(BigDecimal.ONE));
    assertEquals("-1 Hz", fmt.format(BigDecimal.valueOf(-1)));
    assertEquals("1 kHz", fmt.format(BigDecimal.valueOf(1000)));
    assertEquals("1.1 kHz", fmt.format(BigDecimal.valueOf(1100)));
    assertEquals("999.999 kHz", fmt.format(BigDecimal.valueOf(999999)));
    assertEquals("42.42 ZHz",
      fmt.format(BigDecimal.valueOf(4242).scaleByPowerOfTen(19)));
    assertEquals("1000 YHz",
      fmt.format(BigDecimal.ONE.scaleByPowerOfTen(27)));
  }


  @Test
  public void testFormatBigDecimal2()
  {
    StringBuilder expected = new StringBuilder();

    for (int log10 = 0; log10 <= 29; log10++)
    {
      int magnitude = clamp(log10 / 3, MAGNITUDE_MIN, MAGNITUDE_MAX);
      BigDecimal n = BigDecimal.ONE.scaleByPowerOfTen(log10);
      BigDecimal nScaled = BigDecimal.ONE.scaleByPowerOfTen(log10 - magnitude * 3);

      expected.setLength(0);
      expected.append('-').append(nScaled.longValueExact()).append(' ');
      if (magnitude != 0)
        expected.append(MAGNITUDE_CHAR_MAP.get(magnitude).charValue());
      expected.append(UNIT);
      String result1 = fmt.format(n);
      assertEquals(expected.substring(1), result1);

      String result2 = fmt.format(n.negate());
      assertEquals(expected.toString(), result2);
    }
  }


  @Test
  public void testFormatBigDecimal3()
  {
    StringBuilder expected = new StringBuilder();

    for (int log10 = -1; log10 >= -25; log10--)
    {
      int magnitude = clamp((log10 - 2) / 3, MAGNITUDE_MIN, MAGNITUDE_MAX);
      BigDecimal n = BigDecimal.ONE.scaleByPowerOfTen(log10);
      BigDecimal nScaled = BigDecimal.ONE.scaleByPowerOfTen(log10 - magnitude * 3);

      expected.setLength(0);
      expected.append('-').append(nScaled.toPlainString()).append(' ');
      if (magnitude != 0)
        expected.append(MAGNITUDE_CHAR_MAP.get(magnitude).charValue());
      expected.append(UNIT);
      String result1 = fmt.format(n);
      assertEquals(expected.substring(1), result1);

      String result2 = fmt.format(n.negate());
      assertEquals(expected.toString(), result2);
    }
  }


  @Test
  public void testFormatBigDecimal4()
  {
    BigDecimal tenPointZeroOne =
      BigDecimal.valueOf(1001).scaleByPowerOfTen(-2);

    assertEquals("10.01 μHz",
      fmt.format(tenPointZeroOne.scaleByPowerOfTen(-6)));
    assertEquals("10.01 zHz",
      fmt.format(tenPointZeroOne.scaleByPowerOfTen(-21)));
  }


  @Test
  public void testParseLong1() throws ParseException
  {
    assertEquals(0L, fmt.parse("0"));
    assertEquals(0L, fmt.parse("0Hz"));
    assertEquals(0L, fmt.parse("0 Hz"));
    assertEquals(0L, fmt.parse("0kHz"));
    assertEquals(0L, fmt.parse("0 kHz"));
    assertEquals(0L, fmt.parse("0 YHz"));
    assertEquals(0L, fmt.parse("0 mHz"));
    assertEquals(0L, fmt.parse("0 yHz"));

    assertEquals(1L, fmt.parse("1"));
    assertEquals(-1L, fmt.parse("-1"));
    assertEquals(1L, fmt.parse("1 Hz"));
    assertEquals(-1L, fmt.parse("-1 Hz"));
    assertEquals(9L, fmt.parse("9 Hz"));
    assertEquals(99L, fmt.parse("99 Hz"));
    assertEquals(999L, fmt.parse("999 Hz"));
    assertEquals(9999L, fmt.parse("9999 Hz"));

    assertEquals(1000L, fmt.parse("1 kHz"));
    assertEquals(-1000L, fmt.parse("-1 kHz"));
    assertEquals(999_000_000L, fmt.parse("999 MHz"));
  }


  @Test
  public void testParseLong2() throws ParseException
  {
    fmt.setUnit(UNIT);
    assertEquals(1L, fmt.parse("1"));
    assertEquals(1L, fmt.parse("1Hz"));
    assertEquals(1000L, fmt.parse("1kHz"));
  }


  @Test
  public void testParseDouble1() throws ParseException
  {
    assertEquals(10.01, fmt.parse("10.01 Hz"));
    assertEquals(1e-3, fmt.parse("1 m"));
    assertEquals(1e-3, fmt.parse("1 mHz"));
    assertEquals(-1e-3, fmt.parse("-1 mHz"));
    assertEquals(9e-6, fmt.parse("9 μHz"));
    assertEquals(99e-9, fmt.parse("99 nHz"));
    assertEquals(999e-12, fmt.parse("999 pHz"));
    assertEquals(9999e-15, fmt.parse("9999 fHz"));
  }


  @Test
  public void testParseBigDecimal1() throws ParseException
  {
    fmt.setParseBigDecimal(true);

    assertThat(fmt.parse("1 ZHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(21)));
    assertThat(fmt.parse("10 ZHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(22)));
    assertThat(fmt.parse("100 ZHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(23)));
    assertThat(fmt.parse("999 ZHz"),
      comparesEqual(BigDecimal.valueOf(999).scaleByPowerOfTen(21)));
    assertThat(fmt.parse("1 YHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(24)));
    assertThat(fmt.parse("10 YHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(25)));
    assertThat(fmt.parse("100 YHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(26)));
    assertThat(fmt.parse("999 YHz"),
      comparesEqual(BigDecimal.valueOf(999).scaleByPowerOfTen(24)));
    assertThat(fmt.parse("1000 YHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(27)));

    assertThat(fmt.parse("-1 ZHz"),
      comparesEqual(BigDecimal.valueOf(-1).scaleByPowerOfTen(21)));
  }


  @Test
  public void testParseBigDecimal2() throws ParseException
  {
    fmt.setParseBigDecimal(true);

    assertThat(fmt.parse("10.01 Hz"), 
      comparesEqual(BigDecimal.valueOf(1001).scaleByPowerOfTen(-2)));
    assertThat(fmt.parse("1 m"), 
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(-3)));
    assertThat(fmt.parse("1 mHz"),
      comparesEqual(BigDecimal.ONE.scaleByPowerOfTen(-3)));
    assertThat(fmt.parse("-1 mHz"),
      comparesEqual(BigDecimal.valueOf(-1).scaleByPowerOfTen(-3)));
    assertThat(fmt.parse("9 μHz"),
      comparesEqual(BigDecimal.valueOf(9).scaleByPowerOfTen(-6)));
    assertThat(fmt.parse("99 nHz"),
      comparesEqual(BigDecimal.valueOf(99).scaleByPowerOfTen(-9)));
    assertThat(fmt.parse("999 pHz"),
      comparesEqual(BigDecimal.valueOf(999).scaleByPowerOfTen(-12)));
    assertThat(fmt.parse("9999 fHz"),
      comparesEqual(BigDecimal.valueOf(9999).scaleByPowerOfTen(-15)));
  }


  @Test(expected = ParseException.class)
  public void testParseEnforceUnit1() throws ParseException
  {
    fmt.enforceUnitWhenParsing = true;
    fmt.parse("0");
  }


  @Test(expected = ParseException.class)
  public void testParseEnforceUnit2() throws ParseException
  {
    fmt.enforceUnitWhenParsing = true;
    fmt.parse("0k");
  }


  @Test(expected = ParseException.class)
  public void testParseEnforceUnit3() throws ParseException
  {
    fmt.enforceUnitWhenParsing = true;
    fmt.setUnit(UNIT);
    fmt.parse("0 Hz");
  }
}
