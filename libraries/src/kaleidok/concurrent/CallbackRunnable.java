package kaleidok.concurrent;


import java.util.concurrent.Callable;


public abstract class CallbackRunnable<V> implements Runnable, Callable<V>
{
  public Callback<V> callback;

  public final void run()
  {
    try {
      V result;
      try {
        result = call();
      } catch (Exception ex) {
        ex.printStackTrace();
        return;
      }

      Callback<V> callback = this.callback;
      if (callback != null)
        callback.call(result);
    } finally {
      dispose();
    }
  }

  protected void dispose() { }
}
