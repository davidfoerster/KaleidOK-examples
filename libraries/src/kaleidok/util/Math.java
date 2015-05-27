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
    if (offset < 0 || offset > a.length)
      throw new ArrayIndexOutOfBoundsException("offset");
    if (len < 0)
      throw new ArrayIndexOutOfBoundsException("len");
    if (offset + len > a.length)
      throw new ArrayIndexOutOfBoundsException("offset+len");

    return (len != 0) ? sumOfSquares_noBoundsCheck(a, offset, len) : 0;
  }

  private static float sumOfSquares_noBoundsCheck( float[] a, int offset, int len )
  {
    assert offset >= 0 && len > 0;
    assert offset + len <= a.length;

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
    return (a.getSize() != 0) ? sum_noBoundsCheck(a, 0, a.getSize()) : 0;
  }

  public static float sum( FloatList a, int offset, int len )
  {
    if (offset < 0 || offset > a.getSize())
      throw new ArrayIndexOutOfBoundsException("offset");
    if (len < 0)
      throw new ArrayIndexOutOfBoundsException("len");
    if (offset + len > a.getSize())
      throw new ArrayIndexOutOfBoundsException("offset+len");

    return (len != 0) ? sum_noBoundsCheck(a, offset, len) : 0;
  }

  private static float sum_noBoundsCheck( FloatList a, int offset, int len )
  {
    assert offset >= 0 && len > 0;
    assert offset + len <= a.getSize();

    if (len == 1)
      return a.get(offset);

    int halfLen = len / 2;
    return sum_noBoundsCheck(a, offset, halfLen) +
      sum_noBoundsCheck(a, offset + halfLen, len - halfLen);
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
}
