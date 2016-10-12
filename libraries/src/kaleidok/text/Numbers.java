package kaleidok.text;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;


public final class Numbers
{
  private Numbers() { }


  /*
   * TODO: Try to skip BigDecimal and use IEEE floating-point arithmetic in
   * combination with a precision limit instead.
   */
  public static NumberFormat adjustFractionalDigits( NumberFormat fmt,
    double... values )
    throws ArithmeticException
  {
    int fractionalDigits = getFractionalDigits(
      (fmt instanceof DecimalFormat) ? ((DecimalFormat) fmt).getMultiplier() : 1,
      values);
    fmt.setMinimumFractionDigits(fractionalDigits);
    fmt.setMaximumFractionDigits(fractionalDigits);
    return fmt;
  }


  private static int getFractionalDigits( int multiplier, double... values )
    throws ArithmeticException
  {
    if (values.length == 0)
      throw new ArrayIndexOutOfBoundsException("empty array");
    for (double x: values)
      checkFinite(x);
    if (multiplier == 0)
      return 0;

    Stream<BigDecimal> valueStream =
      DoubleStream.of(values)
        .filter(kaleidok.util.Math::isFractional)
        .mapToObj(BigDecimal::valueOf);

    switch (multiplier)
    {
    case 1:
      break;

    case -1:
      valueStream = valueStream.map(BigDecimal::negate);
      break;

    default:
      double dMultiplierLog10 = Math.log10(Math.abs((double) multiplier));
      final int iMultiplierLog10 = (int) dMultiplierLog10;
      //assert Double.isFinite(dMultiplierLog10) || iMultiplierLog10 != dMultiplierLog10;  // This is guaranteed because non-finite floating-point numbers are converted to either 0 or INT_MAX/INT_MIN all of which are unequal to non-finite floating-point numbers.
      if (iMultiplierLog10 == dMultiplierLog10)
      {
        valueStream = valueStream.map(
          (v) -> v.scaleByPowerOfTen(iMultiplierLog10));
        if (multiplier < 0)
          valueStream = valueStream.map(BigDecimal::negate);
      }
      else
      {
        valueStream = valueStream.map(
          BigDecimal.valueOf(multiplier)::multiply);
      }
      break;
    }

    return valueStream
      .mapToInt(Numbers::getFractionalDigitCount)
      .max().orElse(0);
  }


  /**
   * @param x  A number
   * @return  The number of decimal digits necessary to represent x accurately
   * @throws ArithmeticException  if x is non-finite
   * @see #getFractionalDigitCount(BigDecimal)
   */
  public static int getFractionalDigitCount( double x )
    throws ArithmeticException
  {
    checkFinite(x);
    return kaleidok.util.Math.isFractional(x) ?
      getFractionalDigitCount(BigDecimal.valueOf(x)) :
      0;
  }


  /**
   * @param x  A number
   * @return  The number of decimal digits necessary to represent x accurately
   */
  public static int getFractionalDigitCount( BigDecimal x )
  {
    return Math.max(x.stripTrailingZeros().scale(), 0);
  }


  private static void checkFinite( double x )
    throws ArithmeticException
  {
    if (!Double.isFinite(x))
      throw new ArithmeticException("non-finite");
  }
}
