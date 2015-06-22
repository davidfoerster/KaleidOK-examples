package kaleidok.concurrent;

import org.apache.http.concurrent.FutureCallback;


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
}
