package kaleidok.util;

import java.util.Random;


public final class Arrays
{
  private Arrays() { }

  public static int[] shuffle( int[] ar, Random rnd )
  {
    ar = ar.clone();

    for (int i = ar.length - 1; i > 0; i--)
    {
      int index = rnd.nextInt(i + 1);

      // Simple swap
      int a = ar[index];
      ar[index] = ar[i];
      ar[i] = a;
    }

    return ar;
  }
}
