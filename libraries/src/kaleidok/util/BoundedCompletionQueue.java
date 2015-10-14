package kaleidok.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import static org.apache.commons.collections4.iterators.UnmodifiableIterator.unmodifiableIterator;


public class BoundedCompletionQueue<E> implements Queue<E>
{
  private final Queue<E> underlying;

  public final int maxPermits;

  private int permits, completed = 0;


  private BoundedCompletionQueue( Queue<E> underlying, int permits )
  {
    this.underlying = underlying;
    this.maxPermits = permits;
    this.permits = permits;
  }

  public BoundedCompletionQueue( int permits )
  {
    this(new ArrayDeque<E>(), permits);
  }

  public BoundedCompletionQueue( int permits, int capacity )
  {
    this(new ArrayDeque<E>(capacity), permits);
  }

  public BoundedCompletionQueue( int permits, Collection<? extends E> other )
  {
    this(new ArrayDeque<E>(other), permits);
  }


  public int availablePermits()
  {
    return permits;
  }


  public synchronized void release( int permits )
  {
    if (permits < 0)
      throw new IllegalArgumentException(Integer.toString(permits));

    this.permits += permits;
    if (this.permits > maxPermits)
      throw new IllegalStateException("Too many permits returned");
  }

  public void release()
  {
    release(1);
  }


  public synchronized void completeItems( int n )
  {
    if (n < 0)
      throw new IllegalArgumentException(Integer.toString(n));

    completed += n;
    if (completed >= maxPermits) {
      if (completed > maxPermits)
        throw new IllegalStateException("More completed than permitted");
      clear();
    }
  }

  public void completeItem()
  {
    completeItems(1);
  }


  @Override
  public synchronized int size()
  {
    return underlying.size();
  }

  @Override
  public synchronized boolean isEmpty()
  {
    return underlying.isEmpty();
  }

  @Override
  public synchronized boolean contains( Object o )
  {
    return underlying.contains(o);
  }

  @Override
  public synchronized Iterator<E> iterator()
  {
    return unmodifiableIterator(underlying.iterator());
  }

  @Override
  public synchronized Object[] toArray()
  {
    return underlying.toArray();
  }

  @Override
  public synchronized <T> T[] toArray( T[] a )
  {
    return underlying.toArray(a);
  }

  @Override
  public synchronized boolean remove( Object o )
  {
    return underlying.remove(o);
  }

  @Override
  public synchronized boolean containsAll( Collection<?> c )
  {
    return underlying.containsAll(c);
  }

  @Override
  public synchronized boolean addAll( Collection<? extends E> c )
  {
    return underlying.addAll(c);
  }

  @Override
  public synchronized boolean removeAll( Collection<?> c )
  {
    return underlying.removeAll(c);
  }

  @Override
  public synchronized boolean retainAll( Collection<?> c )
  {
    return underlying.retainAll(c);
  }

  @Override
  public synchronized void clear()
  {
    underlying.clear();
  }

  @Override
  public synchronized boolean add( E e )
  {
    return underlying.add(e);
  }

  @Override
  public synchronized boolean offer( E e )
  {
    return underlying.offer(e);
  }

  @Override
  public synchronized E remove()
  {
    if (permits >= 0) {
      if (permits <= 0 || underlying.isEmpty())
        throw new NoSuchElementException();
      permits--;
    }
    return underlying.remove();
  }

  @Override
  public synchronized E poll()
  {
    if (permits >= 0) {
      if (permits <= 0 || underlying.isEmpty())
        return null;
      permits--;
    }
    return underlying.poll();
  }

  @Override
  public synchronized E element()
  {
    return underlying.element();
  }

  @Override
  public synchronized E peek()
  {
    return underlying.peek();
  }
}
