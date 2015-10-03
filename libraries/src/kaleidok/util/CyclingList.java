package kaleidok.util;

import java.util.*;


public class CyclingList<T> implements List<T>
{
  private final List<T> list;

  private int nextIdx;


  public CyclingList( List<T> list, int nextIdx )
  {
    this.list = list;
    setNext(nextIdx);
  }

  public CyclingList( List<T> list )
  {
    this.list = list;
    nextIdx = -1;
  }


  public void setNext( int idx )
  {
    this.nextIdx = java.lang.Math.max(idx, -1);
  }

  public T getNext()
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
  public Iterator<T> iterator()
  {
    return list.iterator();
  }

  @Override
  public Object[] toArray()
  {
    return list.toArray();
  }

  @Override
  public <T1> T1[] toArray( T1[] a )
  {
    return list.toArray(a);
  }

  @Override
  public boolean add( T t )
  {
    return list.add(t);
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
  public boolean addAll( Collection<? extends T> c )
  {
    return list.addAll(c);
  }

  @Override
  public boolean addAll( int index, Collection<? extends T> c )
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
    return list.equals(o);
  }

  @Override
  public int hashCode()
  {
    return list.hashCode();
  }

  @Override
  public T get( int index )
  {
    return list.get(index);
  }

  @Override
  public T set( int index, T element )
  {
    return list.set(index, element);
  }

  @Override
  public void add( int index, T element )
  {
    list.add(index, element);
  }

  @Override
  public T remove( int index )
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
  public ListIterator<T> listIterator()
  {
    return list.listIterator();
  }

  @Override
  public ListIterator<T> listIterator( int index )
  {
    return list.listIterator(index);
  }

  @Override
  public List<T> subList( int fromIndex, int toIndex )
  {
    return list.subList(fromIndex, toIndex);
  }
}
