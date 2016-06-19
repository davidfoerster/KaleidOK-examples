package kaleidok.util;

import java.lang.reflect.Array;
import java.util.*;


public final class Arrays
{
  private Arrays() { }


  @SuppressWarnings("unused")
  public static final Object[] EMPTY_OBJECTS = {};

  public static final Class<?>[] EMPTY_CLASSES = {};


  public static int[] shuffle( int[] ar, Random rnd )
  {
    ar = ar.clone();

    for (int i = ar.length - 1; i > 0; i--)
      swap(ar, i, rnd.nextInt(i + 1));

    return ar;
  }


  public static void swap( int[] ar, int i, int j )
  {
    int e = ar[j];
    ar[j] = ar[i];
    ar[i] = e;
  }


  @SafeVarargs
  public static <E> List<E> asImmutableList( E... a )
  {
    return new ImmutableArrayList<>(a);
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
      final E[] a = this.a;

      for (int i = 0; i < a.length; i++) {
        if (Objects.equals(o, a[i]))
          return i;
      }

      return -1;
    }


    @Override
    public int lastIndexOf( final Object o )
    {
      final E[] a = this.a;

      for (int i = a.length - 1; i >= 0; i--) {
        if (Objects.equals(o, a[i]))
          return i;
      }

      return -1;
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

      if (ol instanceof RandomAccess) {
        for (int i = 0; i < a.length; i++) {
          if (!Objects.equals(a[i], ol.get(i)))
            return false;
        }
      } else {
        int i = 0;
        final Iterator<?> it = ol.iterator();
        while (i < a.length) {
          if (!Objects.equals(a[i++], it.next()))
            return false;
        }
      }

      return true;
    }


    @Override
    public int hashCode()
    {
      int hashCode = 1;
      for (E e: a)
        hashCode = 31 * hashCode + ((e != null) ? e.hashCode() : 0);
      return hashCode;
    }


    @Override
    public boolean contains( Object o )
    {
      return indexOf(o) >= 0;
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
      java.util.Arrays.fill(dst, src.length, dst.length, null);
      return dst;
    }
  }
}
