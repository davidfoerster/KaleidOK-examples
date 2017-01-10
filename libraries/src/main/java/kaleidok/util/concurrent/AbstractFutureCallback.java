package kaleidok.util.concurrent;

import org.apache.http.concurrent.FutureCallback;

import java.util.Objects;
import java.util.function.Consumer;


public abstract class AbstractFutureCallback<T> implements FutureCallback<T>
{
  @Override
  public void failed( Exception ex )
  {
    if (ex != null) {
      Thread currentThread = Thread.currentThread();
      currentThread.getUncaughtExceptionHandler()
        .uncaughtException(currentThread, ex);
    }
  }


  @Override
  public void cancelled()
  {
    // ignored
  }


  public static <T> AbstractFutureCallback<T> getInstance(
    final Consumer<? super T> completionCallback )
  {
    Objects.requireNonNull(completionCallback);
    return new AbstractFutureCallback<T>()
      {
        @Override
        public void completed( T result )
        {
          completionCallback.accept(result);
        }
      };
  }
}
