package kaleidok.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static kaleidok.util.Math.*;


public class MathTest
{
  private static final float[] floatArray = {
      0, 2, 4, 7, -15, -4, 0, 23, 1, 1, 0, 42
    };

  private static final float expected = 2605f;

  public static final float delta = 1e-6f;
  

  @Test
  public void testSumOfSquares1()
  {
    assertEquals(expected, sumOfSquares(floatArray), delta);
  }


  @Test
  public void testSumOfSquares2()
  {
    assertEquals(expected - square(floatArray[floatArray.length-1]),
      sumOfSquares(floatArray, 0, floatArray.length - 1), delta);
    assertEquals(expected - square(floatArray[0]),
      sumOfSquares(floatArray, 1, floatArray.length - 1), delta);
  }


  @Test
  public void testSumOfSquares3()
  {
    assertEquals(square(floatArray[0]), sumOfSquares(floatArray, 0, 1), 0f);
    assertEquals(square(floatArray[1]), sumOfSquares(floatArray, 1, 1), 0f);
    assertEquals(square(floatArray[floatArray.length - 1]),
      sumOfSquares(floatArray, floatArray.length - 1, 1), 0f);
  }


  @Test
  public void testSumOfSquares4()
  {
    assertEquals(0f, sumOfSquares(floatArray, 0, 0), 0f);
    assertEquals(0f, sumOfSquares(floatArray, floatArray.length / 2, 0), 0f);
    assertEquals(0f, sumOfSquares(floatArray, floatArray.length - 1, 0), 0f);
  }
}
