package kaleidok.text;

import kaleidok.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static kaleidok.util.Math.clamp;


public class InternationalSystemOfUnitsFormat extends DecimalFormatDelegator
{
  private static final long serialVersionUID = -5132858851561161728L;

  private String unit;

  public boolean enforceUnitWhenParsing = false;


  public InternationalSystemOfUnitsFormat( DecimalFormat underlying,
    String unit )
  {
    super(underlying);
    setUnit(unit);
  }


  public InternationalSystemOfUnitsFormat( DecimalFormat underlying )
  {
    this(underlying, "");
  }


  public InternationalSystemOfUnitsFormat( String unit )
  {
    this(new DecimalFormat(), unit);
  }


  public InternationalSystemOfUnitsFormat()
  {
    this("");
  }


  public static InternationalSystemOfUnitsFormat getNumberInstance(
    String unit )
  {
    return new InternationalSystemOfUnitsFormat(
      (DecimalFormat) NumberFormat.getNumberInstance(), unit);
  }


  public static InternationalSystemOfUnitsFormat getNumberInstance()
  {
    return getNumberInstance("");
  }


  @Override
  public InternationalSystemOfUnitsFormat clone()
  {
    return (InternationalSystemOfUnitsFormat) super.clone();
  }


  public final String getUnit()
  {
    return unit;
  }

  public final void setUnit( String unit )
  {
    this.unit = (unit != null) ? unit : "";
  }


  @Override
  public StringBuffer format( Object number, StringBuffer toAppendTo,
    FieldPosition pos )
    throws IllegalArgumentException
  {
    if (number instanceof Long || number instanceof Integer ||
      number instanceof Short || number instanceof Byte ||
      number instanceof AtomicInteger || number instanceof AtomicLong ||
      (number instanceof BigInteger && ((BigInteger)number).bitLength () < 64))
    {
      return format(((Number) number).longValue(), toAppendTo, pos);
    }
    if (number instanceof Double || number instanceof Float)
      return format(((Number)number).doubleValue(), toAppendTo, pos);
    if (number instanceof BigInteger)
      return format((BigInteger) number, toAppendTo, pos);
    if (number instanceof BigDecimal)
      return format((BigDecimal) number, toAppendTo, pos);

    throw new IllegalArgumentException(String.format(
      "Cannot format Object of type %s as a Number",
      (number != null) ? number.getClass().getName() : "null"));
  }


  @Override
  public StringBuffer format( long n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    long factor = FACTORS_LONG[magnitude];
    if (n % factor == 0)
    {
      getUnderlying().format(n / factor, toAppendTo, pos);
    }
    else
    {
      getUnderlying().format(
        applyMagnitudeImpl(BigInteger.valueOf(n), -magnitude), toAppendTo, pos);
    }
    return addUnit(toAppendTo, magnitude);
  }


  static int getMagnitude( long n )
  {
    return (n != 0) ? (int)(Math.log10(Math.abs(n)) / 3) : 0;
  }


  @Override
  public StringBuffer format( double n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    return addUnit(
      getUnderlying().format(
        n * Math.pow(1000, -magnitude), toAppendTo, pos),
      magnitude);
  }


  static int getMagnitude( double n )
  {
    double nAbs = Math.abs(n);
    return  (nAbs != 0 && nAbs <= Double.MAX_VALUE) ?
      clamp((int) Math.floor(Math.log10(nAbs) / 3),
        MAGNITUDE_MIN, MAGNITUDE_MAX) :
      0;
  }


  public StringBuffer format( BigInteger n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    return addUnit(
      getUnderlying().format(
        (magnitude != 0) ? applyMagnitudeImpl(n, -magnitude) : 0,
        toAppendTo, pos),
      magnitude);

  }


  static int getMagnitude( BigInteger n )
  {
    return (n.bitLength() < Long.SIZE) ?
      getMagnitude(n.longValue()) :
      getMagnitude(n.doubleValue());
  }


  public StringBuffer format( BigDecimal n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    return addUnit(
      getUnderlying().format(applyMagnitude(n, -magnitude), toAppendTo, pos),
      magnitude);
  }


  static int getMagnitude( BigDecimal n )
  {
    n = n.stripTrailingZeros();
    int significantDigitPosition = n.precision() - n.scale();
    return clamp(
      (significantDigitPosition - ((significantDigitPosition > 0) ? 1 : 3)) / 3,
      MAGNITUDE_MIN, MAGNITUDE_MAX);
  }


  private StringBuffer addUnit( StringBuffer sb, int magnitude )
  {
    String unit = this.unit;
    int unitOffset = Strings.countLeadingWhitespace(unit, 0);
    if (magnitude != 0)
    {
      sb.append(unit, 0, unitOffset)
        .append(getMagnitudeChar(magnitude))
        .append(unit, unitOffset, unit.length());
    }
    else
    {
      sb.append(unit);
    }
    return sb;
  }


  @Override
  public Number parse( String text, ParsePosition parsePosition )
  {
    Number result = getUnderlying().parse(text, parsePosition);
    int pos = parsePosition.getIndex();
    if (pos == 0)
      return result;

    int len = text.length();
    String unit = this.unit;
    int unitLen = unit.length();
    int unitOffset = Strings.countLeadingWhitespace(unit, 0);
    if (unitOffset != 0)
      pos = Strings.countLeadingWhitespace(text, pos);

    int magnitude = (pos < len) ? getMagnitudeForChar(text.charAt(pos)) : 0;
    if (magnitude != 0)
      pos++;

    if (text.regionMatches(pos, unit, unitOffset, unitLen - unitOffset))
    {
      pos += unitLen - unitOffset;
    }
    else if (enforceUnitWhenParsing)
    {
      parsePosition.setIndex(0);
      return null;
    }

    parsePosition.setIndex(pos);
    return
      (magnitude == 0) ?
        result :
      (result instanceof Long) ?
        applyMagnitude((Long) result, magnitude) :
      (result instanceof Double) ?
        applyMagnitude((Double) result, magnitude) :
      (result instanceof BigInteger) ?
        applyMagnitude((BigInteger) result, magnitude) :
      (result instanceof BigDecimal) ?
        applyMagnitude((BigDecimal) result, magnitude) :
        throwUnsupportedNumberType(result);
  }


  private Number applyMagnitude( Long n, int magnitude )
  {
    return (n != 0) ? applyMagnitudeImpl(n, magnitude) : n;
  }

  private Number applyMagnitudeImpl( long n, int magnitude )
  {
    final long[] factors = FACTORS_LONG;
    return
      (magnitude >= 0 && magnitude < FACTORS_LONG_LENGTH &&
          n <= Long.MAX_VALUE / factors[magnitude]) ?
        n * factors[magnitude] :
      (magnitude <= 0 && magnitude > -FACTORS_LONG_LENGTH &&
          n % factors[-magnitude] == 0) ?
        n / factors[-magnitude] :
      isParseBigDecimal() ?
        applyMagnitudeImpl(BigInteger.valueOf(n), magnitude) :
        applyMagnitudeImpl((double) n, magnitude);
  }


  private static Number applyMagnitude( BigInteger n, int magnitude )
  {
    return BigInteger.ZERO.equals(n) ? n : applyMagnitudeImpl(n, magnitude);
  }


  private static Number applyMagnitudeImpl( BigInteger n, int magnitude )
  {
    return new BigDecimal(n, magnitude * -3);
  }


  private Number applyMagnitude( Double x, int magnitude )
  {
    return (x != 0) ? applyMagnitudeImpl(x, magnitude) : x;

  }

  private Number applyMagnitudeImpl( double x, int magnitude )
  {
    return isParseBigDecimal() ?
      BigDecimal.valueOf(x).scaleByPowerOfTen(magnitude * 3) :
      x * Math.pow(1000, magnitude);
  }


  private static Number applyMagnitude( BigDecimal x, int magnitude )
  {
    return (x.signum() != 0) ?
      x.scaleByPowerOfTen(magnitude * 3) :
      x;
  }


  private static Number throwUnsupportedNumberType( Number x )
  {
    throw new AssertionError(
      "Unexpected and unsupported Number type " + x.getClass().getName());
  }


  public static int getMagnitudeForChar( char c )
  {
    return CHAR_MAGNITUDE_MAP.getOrDefault((c != 'μ') ? c : 'u', 0);
  }

  public static char getMagnitudeChar( int magnitude )
    throws IllegalArgumentException
  {
    Character c = MAGNITUDE_CHAR_MAP.get(magnitude);
    if (c != null)
      return c;

    throw new IllegalArgumentException(
      "Unsupported magnitude: 1000^" + magnitude);
  }


  public static final int MAGNITUDE_MIN = -8, MAGNITUDE_MAX = 8;

  public static final Map<Integer, Character> MAGNITUDE_CHAR_MAP;

  public static final Map<Character, Integer> CHAR_MAGNITUDE_MAP;

  static
  {
    Map<Integer, Character> m = new HashMap<>(24);
    m.put(-8, 'y');
    m.put(-7, 'z');
    m.put(-6, 'a');
    m.put(-5, 'f');
    m.put(-4, 'p');
    m.put(-3, 'n');
    m.put(-2, 'μ');
    m.put(-1, 'm');
    m.put(1, 'k');
    m.put(2, 'M');
    m.put(3, 'G');
    m.put(4, 'T');
    m.put(5, 'P');
    m.put(6, 'E');
    m.put(7, 'Z');
    m.put(8, 'Y');
    MAGNITUDE_CHAR_MAP = Collections.unmodifiableMap(m);

    Map<Character, Integer> cmm =
      m.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    cmm.put('u', -2);
    CHAR_MAGNITUDE_MAP = Collections.unmodifiableMap(cmm);
  }

  private static final int FACTORS_LONG_LENGTH = 7;

  private static final long[] FACTORS_LONG = {
    1L, 1000L, 1_000_000L, 1_000_000_000L, 1_000_000_000_000L,
    1_000_000_000_000_000L, 1_000_000_000_000_000_000L
  };
}
