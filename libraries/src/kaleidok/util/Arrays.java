package kaleidok.util;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;


public final class Arrays
{
  private Arrays() { }


  public static int[] shuffle( int[] ar, Random rnd )
  {
    ar = ar.clone();

    for (int i = ar.length - 1; i > 0; i--)
      ArrayUtils.swap(ar, i, rnd.nextInt(i + 1));

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


  @SafeVarargs
  public static <E> List<E> asImmutableList( E... a )
  {
    return (a.length != 0) ?
      new ImmutableArrayList<>(a) :
      Collections.emptyList();
  }


  public static class ImmutableArrayList<E> extends AbstractList<E>
    implements RandomAccess
  {
    private final E[] a;


    public ImmutableArrayList( E[] a )
    {
      if (a == null)
        throw new NullPointerException();
      this.a = a;
    }


    @Override
    public E get( int index )
    {
      return a[index];
    }


    @Override
    public int size()
    {
      return a.length;
    }


    @Override
    public int indexOf( final Object o )
    {
      return ArrayUtils.indexOf(a, o);
    }


    @Override
    public int lastIndexOf( final Object o )
    {
      return ArrayUtils.lastIndexOf(a, o);
    }


    @Override
    public boolean equals( Object o )
    {
      if (o == this)
        return true;
      if (!(o instanceof List))
        return false;

      final List<?> ol = (List<?>) o;
      final E[] a = this.a;
      if (ol.size() != a.length)
        return false;

      if (ol instanceof ImmutableArrayList)
        return java.util.Arrays.equals(a, ((ImmutableArrayList<?>) ol).a);
      if (ol instanceof RandomAccess)
      {
        for (int i = 0; i < a.length; i++)
        {
          if (!Objects.equals(a[i], ol.get(i)))
            return false;
        }
      }
      else
      {
        int i = 0;
        final Iterator<?> it = ol.iterator();
        while (i < a.length)
        {
          if (!Objects.equals(a[i++], it.next()))
            return false;
        }
      }

      return true;
    }


    @Override
    public int hashCode()
    {
      return java.util.Arrays.hashCode(a);
    }


    @Override
    public boolean contains( Object o )
    {
      return ArrayUtils.contains(a, o);
    }


    @Override
    public Object[] toArray()
    {
      return java.util.Arrays.copyOf(a, a.length, Object[].class);
    }


    @Override
    public <T> T[] toArray( T[] dst )
    {
      final E[] src = this.a;
      if (dst.length < src.length) {
        //noinspection unchecked
        dst = (T[]) Array.newInstance(dst.getClass().getComponentType(), src.length);
      }
      //noinspection SuspiciousSystemArraycopy
      System.arraycopy(src, 0, dst, 0, src.length);
      if (dst.length > src.length)
        dst[src.length] = null;
      return dst;
    }
  }
}
