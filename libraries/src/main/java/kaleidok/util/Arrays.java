package kaleidok.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;


public final class Arrays
{
  private Arrays() { }


  public static int[] shuffle( int[] ar, Random rnd )
  {
    for (int i = ar.length - 1; i > 0; i--)
    {
      int j = rnd.nextInt(i + 1);
      int aux = ar[i];
      ar[i] = ar[j];
      ar[j] = aux;
    }

    return ar;
  }


  public static DoubleStream stream( final float[] a, int offset, int end )
  {
    return IntStream.range(offset, end).mapToDouble((i) -> a[i]);
  }

  public static DoubleStream stream( final float[] a )
  {
    return stream(a, 0, a.length);
  }


  public static IntStream stream( final char[] a, int offset, int end )
  {
    return IntStream.range(offset, end).map((i) -> a[i]);
  }

  public static IntStream stream( final char[] a )
  {
    return stream(a, 0, a.length);
  }


  public static <T, U> boolean equals( final T[] a, final U[] b,
    final BiPredicate<? super T, ? super U> predicate )
  {
    //noinspection ArrayEquality
    if (a != b)
    {
      if (a == null || b == null)
        return false;

      final int length = a.length;
      if (length != b.length)
        return false;

      for (int i = 0; i < length; i++)
      {
        if (!predicate.test(a[i], b[i]))
          return false;
      }
    }
    return true;
  }


  @SafeVarargs
  public static <E> List<E> asImmutableList( E... a )
  {
    return (a.length != 0) ?
      new ImmutableArrayList<>(a) :
      Collections.emptyList();
  }
}
