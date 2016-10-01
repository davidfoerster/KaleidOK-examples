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
  {
    if (values.length == 0)
      throw new ArrayIndexOutOfBoundsException("empty array");

    Stream<BigDecimal> valueStream =
      DoubleStream.of(values)
        .filter(Double::isFinite)
        .mapToObj(BigDecimal::valueOf);

    if (fmt instanceof DecimalFormat)
    {
      int multiplier = ((DecimalFormat) fmt).getMultiplier();
      if (multiplier != 1)
      {
        double dMultiplierLog10 = Math.log10(Math.abs((long) multiplier));
        final int iMultiplierLog10 = (int) dMultiplierLog10;
        //assert Double.isFinite(dMultiplierLog10) || iMultiplierLog10 != dMultiplierLog10;  // This is guaranteed because non-finite floating-point numbers are converted to either 0 or INT_MAX/INT_MIN all of which are unequal to non-finite floating-point numbers.
        if (iMultiplierLog10 == dMultiplierLog10)
        {
          valueStream = valueStream.map(
            (v) -> v.scaleByPowerOfTen(iMultiplierLog10));
          if (multiplier < 0)
            valueStream = valueStream.map(BigDecimal::negate);
        }
        else if (multiplier == -1)
        {
          valueStream = valueStream.map(BigDecimal::negate);
        }
        else
        {
          valueStream = valueStream.map(
            BigDecimal.valueOf(multiplier)::multiply);
        }
      }
    }

    int fractionalDigits = valueStream
      .mapToInt(Numbers::getFractionalDigits)
      .max().orElseThrow(Numbers::nonFiniteException);

    fmt.setMinimumFractionDigits(fractionalDigits);
    fmt.setMaximumFractionDigits(fractionalDigits);
    return fmt;
  }


  public static int getFractionalDigits( double x )
  {
    if (!Double.isFinite(x))
      throw nonFiniteException();

    return (x != (long) x) ?
      getFractionalDigits(BigDecimal.valueOf(x)) :
      0;
  }


  public static int getFractionalDigits( BigDecimal x )
  {
    return Math.max(x.stripTrailingZeros().scale(), 0);
  }


  private static ArithmeticException nonFiniteException()
  {
    return new ArithmeticException("non-finite");
  }
}
