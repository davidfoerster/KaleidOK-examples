package kaleidok.text;

import kaleidok.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static kaleidok.util.Math.clamp;
import static kaleidok.util.Math.pow10;


public class InternationalSystemOfUnitsFormat extends DecimalFormatDelegator
{
  private static final long serialVersionUID = -5132858851561161728L;

  private String unit;

  public boolean enforceUnitWhenParsing = false;

  private int minMagnitude = MAGNITUDE_MIN, maxMagnitude = MAGNITUDE_MAX;


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


  public void setMagnitudeBounds( int min, int max )
  {
    if (min < MAGNITUDE_MIN)
    {
      throw new IllegalArgumentException(
        min + " is below the permissible minimum " + MAGNITUDE_MIN);
    }
    if (max > MAGNITUDE_MAX)
    {
      throw new IllegalArgumentException(
        max + " exceeds the permissible maximum " + MAGNITUDE_MAX);
    }
    if (min > max)
    {
      throw new IllegalArgumentException(min + " > " + max);
    }

    minMagnitude = min;
    maxMagnitude = max;
  }


  public int getMinMagnitude()
  {
    return minMagnitude;
  }


  public int getMaxMagnitude()
  {
    return maxMagnitude;
  }


  @Override
  public StringBuffer format( Object o, StringBuffer toAppendTo,
    FieldPosition pos )
    throws IllegalArgumentException
  {
    if (o instanceof Number)
    {
      Number number = (Number) o;

      if (kaleidok.util.Math.isBasicIntegral(number))
        return format(number.longValue(), toAppendTo, pos);
      if (number instanceof Double || number instanceof Float)
        return format(number.doubleValue(), toAppendTo, pos);
      if (number instanceof BigInteger)
        return format((BigInteger) number, toAppendTo, pos);
      if (number instanceof BigDecimal)
        return format((BigDecimal) number, toAppendTo, pos);
    }

    throw new IllegalArgumentException(String.format(
      "Cannot format Object of type %s as a Number",
      (o != null) ? o.getClass().getName() : null));
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


  int getMagnitude( long n )
  {
    if (minMagnitude == maxMagnitude)
      return minMagnitude;

    n = Math.abs(n);
    return clamp(
        (n < 1000) ?
          ((n >= 0) ? 0 : (FACTORS_LONG_LENGTH - 1)) :
          getMagnitudeImpl(n),
      minMagnitude, maxMagnitude);
  }


  private static int getMagnitudeImpl( long n )
  {
    int magnitude = Arrays.binarySearch(
      FACTORS_LONG, 2, FACTORS_LONG_LENGTH, n);
    return (magnitude < 0) ? -2 - magnitude : magnitude;
  }


  @Override
  public StringBuffer format( double n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    return addUnit(
      getUnderlying().format(
        n * pow10(magnitude * -3), toAppendTo, pos),
      magnitude);
  }


  int getMagnitude( double n )
  {
    if (minMagnitude == maxMagnitude)
      return minMagnitude;

    n = Math.abs(n);
    return clamp(
      (n != 0 && n <= Double.MAX_VALUE) ?
        (int) Math.floor(Math.log10(n) / 3) :
        0,
      minMagnitude, maxMagnitude);
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


  int getMagnitude( BigInteger n )
  {
    return getMagnitude(n.doubleValue());
  }


  public StringBuffer format( BigDecimal n, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    int magnitude = getMagnitude(n);
    return addUnit(
      getUnderlying().format(applyMagnitude(n, -magnitude), toAppendTo, pos),
      magnitude);
  }


  int getMagnitude( BigDecimal n )
  {
    if (minMagnitude == maxMagnitude)
      return minMagnitude;

    n = n.stripTrailingZeros();
    int significantDigitPosition = n.precision() - n.scale();
    return clamp(
      (significantDigitPosition - ((significantDigitPosition > 0) ? 1 : 3)) / 3,
      minMagnitude, maxMagnitude);
  }


  private StringBuffer addUnit( StringBuffer sb, int magnitude )
  {
    if (magnitude != 0)
    {
      String unit = this.unit;
      int unitOffset = Strings.countLeadingWhitespace(unit, 0);
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
    int pos = parsePosition.getIndex();
    Number result = getUnderlying().parse(text, parsePosition);
    if (pos == parsePosition.getIndex())
      return result;

    pos = parsePosition.getIndex();
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
      parsePosition.setErrorIndex(pos);
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
    return (n.signum() != 0) ? applyMagnitudeImpl(n, magnitude) : n;
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
      applyMagnitudeImpl(BigDecimal.valueOf(x), magnitude) :
      x * Math.pow(1000, magnitude);
  }


  private static Number applyMagnitude( BigDecimal x, int magnitude )
  {
    return (x.signum() != 0) ? applyMagnitudeImpl(x, magnitude) : x;
  }

  private static Number applyMagnitudeImpl( BigDecimal x, int magnitude )
  {
    return x.scaleByPowerOfTen(magnitude * 3);
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
    Map<Integer, Character> m =
      new HashMap<>((MAGNITUDE_MAX - MAGNITUDE_MIN) * 3 / 2);
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

  static final int FACTORS_LONG_LENGTH = 7;

  static final long[] FACTORS_LONG = {
    1L, 1000L, 1_000_000L, 1_000_000_000L, 1_000_000_000_000L,
    1_000_000_000_000_000L, 1_000_000_000_000_000_000L
  };
}
