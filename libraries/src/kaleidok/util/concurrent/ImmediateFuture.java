package kaleidok.util.concurrent;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;


public class ImmediateFuture<V> implements RunnableFuture<V>
{
  public static final ImmediateFuture<?> EMPTY = new ImmediateFuture<>(null);


  @SuppressWarnings("unchecked")
  public static <V> ImmediateFuture<V> getEmpty()
  {
    return (ImmediateFuture<V>) EMPTY;
  }


  private final V value;


  public ImmediateFuture( V value )
  {
    this.value = value;
  }


  @Override
  public void run() { }


  @Override
  public boolean cancel( boolean mayInterruptIfRunning )
  {
    return false;
  }


  @Override
  public boolean isCancelled()
  {
    return false;
  }


  @Override
  public boolean isDone()
  {
    return true;
  }


  @Override
  public V get()
  {
    return value;
  }


  @Override
  public V get( long timeout, TimeUnit unit )
  {
    return value;
  }
}
