package kaleidok.util.containers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

import static org.apache.commons.collections4.iterators.UnmodifiableIterator.unmodifiableIterator;


public class BoundedCompletionQueue<E> implements Queue<E>
{
  private final Queue<E> underlying;

  public final int maxPermits;

  private int permits, completed = 0;

  private Collection<E> removedItems;

  public Consumer<Collection<? super E>> completionCallback = null;


  private BoundedCompletionQueue( Queue<E> underlying, int permits, int capacityHint )
  {
    this.underlying = underlying;
    this.maxPermits = permits;
    this.permits = permits;
    this.removedItems = new ArrayList<>(capacityHint);
  }

  public BoundedCompletionQueue( int permits )
  {
    this(new ArrayDeque<>(), permits, 0);
  }

  public BoundedCompletionQueue( int permits, int capacity )
  {
    this(new ArrayDeque<>(capacity), permits, capacity);
  }

  public BoundedCompletionQueue( int permits, Collection<? extends E> other )
  {
    this(new ArrayDeque<>(other), permits, other.size());
  }


  public int availablePermits()
  {
    return permits;
  }


  public int getCompleted()
  {
    return completed;
  }


  public synchronized void release( int n )
  {
    if (n < 0)
      throw new IllegalArgumentException(Integer.toString(n));

    long newPermits = (long) this.permits + n;
    if (newPermits > maxPermits)
    {
      throw new IllegalStateException(String.format(
        "Too many permits returned: %d (max. %d)", n, maxPermits));
    }

    permits = (int) newPermits;

    if (newPermits + completed == maxPermits && this.isEmpty())
      doCompletionCallback();
  }

  public void release()
  {
    release(1);
  }


  public synchronized void completeItems( int n )
  {
    if (n < 0)
      throw new IllegalArgumentException(Integer.toString(n));

    long newCompleted = (long) completed + n;
    if (newCompleted >= maxPermits) {
      if (newCompleted != maxPermits)
      {
        throw new IllegalStateException(String.format(
          "More items completed (%d) than permitted (%d)",
          n, maxPermits - completed));
      }

      clear();
      doCompletionCallback();
    }

    completed = (int) newCompleted;
  }

  public void completeItem()
  {
    completeItems(1);
  }


  protected void doCompletionCallback()
  {
    Consumer<Collection<? super E>> completionCallback =
      this.completionCallback;
    if (completionCallback != null)
      completionCallback.accept(removedItems);
    removedItems = null;
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

  @SuppressWarnings("SuspiciousToArrayCall")
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
