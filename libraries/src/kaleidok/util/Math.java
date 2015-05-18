package kaleidok.util;

public final class Math
{
  private Math() { }

  public static float sumOfSquares( float[] a )
  {
    return (a.length != 0) ? sumOfSquares_noBoundChecks(a, 0, a.length) : 0;
  }

  public static float sumOfSquares( float[] a, int offset, int len )
  {
    if (offset < 0 || offset > a.length)
      throw new ArrayIndexOutOfBoundsException("offset");
    if (len < 0)
      throw new ArrayIndexOutOfBoundsException("len");
    if (offset + len > a.length)
      throw new ArrayIndexOutOfBoundsException("offset+len");

    return (len != 0) ? sumOfSquares_noBoundChecks(a, offset, len) : 0;
  }

  private static float sumOfSquares_noBoundChecks( float[] a, int offset, int len )
  {
    assert offset >= 0 && len > 0;
    assert offset + len <= a.length;

    if (len == 1) {
      float x = a[offset];
      return x * x;
    }

    int halfLen = len / 2;
    return sumOfSquares_noBoundChecks(a, offset, halfLen) +
      sumOfSquares_noBoundChecks(a, offset + halfLen, len - halfLen);
  }

  public static float square( float x )
  {
    return x * x;
  }
}
