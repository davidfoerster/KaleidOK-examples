package kaleidok.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;


public abstract class NotifyFuture<V> implements Future<V>
{
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
    if (timeout == 0) {
      waitFor();
    } else if (timeout < 0) {
      throw new IllegalArgumentException(String.valueOf(timeout));
    } else if (!isDone()) {
      assert unit.convert(Long.MAX_VALUE, NANOSECONDS) >= timeout;
      long now = System.nanoTime();
      assert now <= Long.MAX_VALUE - unit.toNanos(timeout);
      final long endTime = now + unit.toNanos(timeout);

      synchronized (this) {
        if (!isDone()) {
          for (; now < endTime; now = System.nanoTime()) {
            wait(Math.max(NANOSECONDS.toMillis(endTime - now), 1));
            if (isDone())
              return;
          }
          throw new TimeoutException();
        }
      }
    }
  }
}
