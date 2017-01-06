package kaleidok.util;

import java.util.*;

import static java.lang.Math.max;


public class CyclingList<E> implements List<E>
{
  private final List<E> list;

  private int nextIdx;


  public CyclingList( List<E> list, int nextIdx )
  {
    this.list = list;
    this.nextIdx = max(nextIdx, -1);
  }

  public CyclingList( List<E> list )
  {
    this.list = list;
    nextIdx = -1;
  }


  public void setNext( int idx )
  {
    this.nextIdx = max(idx, -1);
  }

  public E getNext()
  {
    int len = this.size();
    if (len == 0)
      throw new NoSuchElementException();

    return get(nextIdx = (nextIdx + 1) % len);
  }


  @Override
  public int size()
  {
    return list.size();
  }

  @Override
  public boolean isEmpty()
  {
    return list.isEmpty();
  }

  @Override
  public boolean contains( Object o )
  {
    return list.contains(o);
  }

  @Override
  public Iterator<E> iterator()
  {
    return list.iterator();
  }

  @Override
  public Object[] toArray()
  {
    return list.toArray();
  }

  @Override
  public <T> T[] toArray( T[] a )
  {
    //noinspection SuspiciousToArrayCall
    return list.toArray(a);
  }

  @Override
  public boolean add( E e )
  {
    return list.add(e);
  }

  @Override
  public boolean remove( Object o )
  {
    return list.remove(o);
  }

  @Override
  public boolean containsAll( Collection<?> c )
  {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll( Collection<? extends E> c )
  {
    return list.addAll(c);
  }

  @Override
  public boolean addAll( int index, Collection<? extends E> c )
  {
    return list.addAll(index, c);
  }

  @Override
  public boolean removeAll( Collection<?> c )
  {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll( Collection<?> c )
  {
    return list.retainAll(c);
  }

  @Override
  public void clear()
  {
    list.clear();
  }

  @Override
  public boolean equals( Object o )
  {
    return o instanceof CyclingList && list.equals(o);
  }

  @Override
  public int hashCode()
  {
    return list.hashCode();
  }

  @Override
  public E get( int index )
  {
    return list.get(index);
  }

  @Override
  public E set( int index, E element )
  {
    return list.set(index, element);
  }

  @Override
  public void add( int index, E element )
  {
    list.add(index, element);
  }

  @Override
  public E remove( int index )
  {
    return list.remove(index);
  }

  @Override
  public int indexOf( Object o )
  {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf( Object o )
  {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator()
  {
    return list.listIterator();
  }

  @Override
  public ListIterator<E> listIterator( int index )
  {
    return list.listIterator(index);
  }

  @Override
  public List<E> subList( int fromIndex, int toIndex )
  {
    return list.subList(fromIndex, toIndex);
  }
}
