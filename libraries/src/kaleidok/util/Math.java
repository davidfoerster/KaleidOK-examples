package kaleidok.util;

import kaleidok.containers.FloatList;

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

    if (len == 1) {
      float x = a[offset];
      return x * x;
    }

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
    assert len != 0 && checkBounds(offset, len, a.size()) :
      getBoundsExceededMessage(offset, len, a.size());

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
    assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }

  public static int clamp( int x, int min, int max )
  {
    assert min <= max : min + " > " + max;
    return min(max(x, min), max);
  }
}
