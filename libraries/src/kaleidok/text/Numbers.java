package kaleidok.text;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static kaleidok.util.AssertionUtils.fastAssert;


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
      (fmt instanceof DecimalFormat) ?
        Math.abs((long) ((DecimalFormat) fmt).getMultiplier()) :
        1,
      values);
    fmt.setMinimumFractionDigits(fractionalDigits);
    fmt.setMaximumFractionDigits(fractionalDigits);
    return fmt;
  }


  private static int getFractionalDigits( long multiplier, double[] values )
    throws ArithmeticException
  {
    if (values.length == 0)
      throw new IllegalArgumentException("empty values array");
    for (double x: values)
      checkFinite(x);
    if (multiplier == 0)
      return 0;

    Stream<BigDecimal> valueStream =
      DoubleStream.of(values)
        .filter(kaleidok.util.Math::isFractional)
        .mapToObj(BigDecimal::valueOf);

    if (multiplier != 1)
    {
      fastAssert(multiplier > 0);

      double dMultiplierLog10 = Math.log10(multiplier);
      final int iMultiplierLog10 = (int) dMultiplierLog10;
      //assert Double.isFinite(dMultiplierLog10) || iMultiplierLog10 != dMultiplierLog10;  // This is guaranteed because non-finite floating-point numbers are converted to either 0 or INT_MAX/INT_MIN all of which are unequal to non-finite floating-point numbers.
      valueStream = valueStream.map(
        (iMultiplierLog10 == dMultiplierLog10) ?
          (v) -> v.scaleByPowerOfTen(iMultiplierLog10) :
          BigDecimal.valueOf(multiplier)::multiply);
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
