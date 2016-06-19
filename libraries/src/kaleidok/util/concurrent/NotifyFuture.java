package kaleidok.util.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;


public abstract class NotifyFuture<V> implements Future<V>
{
  @SuppressWarnings("WaitNotInLoop")
  protected void waitFor() throws InterruptedException
  {
    if (!isDone()) {
      synchronized (this) {
        if (!isDone())
          wait();
      }
      assert isDone();
    }
  }

  protected void waitFor( long timeout, TimeUnit unit )
    throws InterruptedException, TimeoutException
  {
    if (timeout == 0)
    {
      waitFor();
    }
    else if (timeout < 0)
    {
      throw new IllegalArgumentException("Negative timeout: " + timeout);
    }
    else if (!isDone())
    {
      assert unit.convert(Long.MAX_VALUE, NANOSECONDS) >= timeout :
        String.format("%d %s overflows when converting to %s",
          timeout, unit, NANOSECONDS);

      timeout = unit.toNanos(timeout);
      // unit = NANOSECONDS;
      long now = System.nanoTime();
      assert now <= Long.MAX_VALUE - timeout;
      final long endTime = now + timeout;

      synchronized (this) {
        //noinspection CallToNativeMethodWhileLocked
        while (!isDone() && (now = System.nanoTime()) < endTime)
          NANOSECONDS.timedWait(this, endTime - now);
      }
      if (now >= endTime)
        throw new TimeoutException();
    }
  }
}
