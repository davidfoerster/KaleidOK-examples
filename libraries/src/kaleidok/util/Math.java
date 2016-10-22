package kaleidok.util;

import kaleidok.util.containers.FloatList;
import sun.misc.DoubleConsts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.*;


public final class Math
{
  private Math() { }

  public static float sumOfSquares( float[] a )
  {
    return (a.length != 0) ? sumOfSquares_noBoundsCheck(a, 0, a.length) : 0;
  }

  public static float sumOfSquares( float[] a, int offset, int len )
  {
    checkBounds(offset, len, a.length);
    return (len != 0) ? sumOfSquares_noBoundsCheck(a, offset, len) : 0;
  }

  private static float sumOfSquares_noBoundsCheck( float[] a, int offset, int len )
  {
    assert len != 0 && checkBounds(offset, len, a.length) :
      getBoundsExceededMessage(offset, len, a.length);

    if (len == 1)
      return square(a[offset]);

    int halfLen = len / 2;
    return sumOfSquares_noBoundsCheck(a, offset, halfLen) +
      sumOfSquares_noBoundsCheck(a, offset + halfLen, len - halfLen);
  }


  public static float sum( FloatList a )
  {
    return (a.size() != 0) ? sum_noBoundsCheck(a, 0, a.size()) : 0;
  }

  public static float sum( FloatList a, int offset, int len )
  {
    checkBounds(offset, len, a.size());
    return (len != 0) ? sum_noBoundsCheck(a, offset, len) : 0;
  }

  private static float sum_noBoundsCheck( FloatList a, int offset, int len )
  {
    /*assert len != 0 && checkBounds(offset, len, a.size()) :
      getBoundsExceededMessage(offset, len, a.size());*/

    if (len == 1)
      return a.get(offset);

    int halfLen = len / 2;
    return sum_noBoundsCheck(a, offset, halfLen) +
      sum_noBoundsCheck(a, offset + halfLen, len - halfLen);
  }


  private static boolean checkBounds( int offset, int len, int bufSize )
  {
    if (offset < 0 || offset > bufSize)
      throwArrayIndexOutOfBoundsException(offset);
    if (len < 0)
      throwArrayIndexOutOfBoundsException(len);
    if ((long) offset + len > bufSize)
      throwArrayIndexOutOfBoundsException((long) offset + len);
    return true;
  }

  private static void throwArrayIndexOutOfBoundsException( long index )
  {
    throw new ArrayIndexOutOfBoundsException(Long.toString(index));
  }

  private static String getBoundsExceededMessage( int offset, int len, int bufSize )
  {
    return String.format(
      "%2$d is 0, %1$d, %2$d or %3$d are negative, or %1$d + %2$d ≥ %3$d",
      offset, len, bufSize);
  }


  public static float square( float x )
  {
    return x * x;
  }


  public static final double LN2 = log(2), LN2_INV = 1 / LN2;

  public static double log2( double x )
  {
    return log(x) * LN2_INV;
  }


  public static boolean isPowerOfTwo( long number )
  {
    if (number >= 0)
      return (number & (number - 1)) == 0;
    throw new IllegalArgumentException("number is negative");
  }


  public static int divCeil( int dividend, int divisor )
  {
    return (dividend - 1) / divisor + 1;
  }


  public static long clamp( long x, long min, long max )
  {
    //assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }

  public static int clamp( int x, int min, int max )
  {
    //assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }

  public static double clamp( double x, double min, double max )
  {
    //assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }

  public static float clamp( float x, float min, float max )
  {
    //assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }


  public static int toIntExact( double x )
  {
    if (x < Integer.MIN_VALUE)
      throw new ArithmeticException("underflow");
    if (x > Integer.MAX_VALUE)
      throw new ArithmeticException("overflow");
    if (Double.isNaN(x))
      throw new ArithmeticException("not a number");

    return (int) x;
  }


  public static float mapNormalized( float x, float left, float right )
  {
    return (x * (right - left) + left) / right;
  }


  public static boolean isBasicIntegral( Number n )
  {
    return
      n instanceof Integer || n instanceof Long || n instanceof Short ||
      n instanceof Byte || n instanceof AtomicInteger || n instanceof AtomicLong ||
      (n instanceof BigInteger && ((BigInteger) n).bitLength() < Long.SIZE);
  }


  public static BigInteger toBigInteger( Number v )
  {
    return
      (v instanceof BigInteger) ?
        (BigInteger) v :
      (v instanceof BigDecimal) ?
        ((BigDecimal) v).toBigInteger() :
        BigInteger.valueOf(v.longValue());
  }


  public static BigDecimal toBigDecimal( Number v )
  {
    return
      (v instanceof BigDecimal) ?
        (BigDecimal) v :
      isBasicIntegral(v) ?
        BigDecimal.valueOf(v.longValue()) :
      (v instanceof BigInteger) ?
        new BigDecimal((BigInteger) v) :
        BigDecimal.valueOf(v.doubleValue());
  }


  /**
   * Tests whether a number has any non-zero digits in its fractional part.
   * <p>
   * Non-finite numbers are <em>not</em> considered fractional.
   *
   * @param x  A number
   * @return  whether that number has a non-zero fractional part
   */
  public static boolean isFractional( double x )
  {
    return (x != (long) x) && // true for all fractional numbers but also "large" integers and non-finites (see below)
      (abs(x) <= (1L << DoubleConsts.SIGNIFICAND_WIDTH)); // false for non-finites and all "double" numbers too large to have a fractional part
  }


  public static double pow10( int exponent )
  {
    if (exponent < 0 || exponent > 18)  // floor(log10(Long.MAX_VALUE)) == 18
      return pow(10, exponent);

    long result = 1;
    for (; exponent != 0; exponent--)
      result = (result << 3) + (result << 1);
    return result;
  }
}
