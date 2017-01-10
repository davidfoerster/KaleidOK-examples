package kaleidok.util.concurrent;

import org.apache.http.concurrent.FutureCallback;

import java.util.Objects;
import java.util.function.BiConsumer;


public abstract class NestedFutureCallback<T, N, NCB extends FutureCallback<? super N>>
  implements FutureCallback<T>
{
  public final NCB nested;


  protected NestedFutureCallback( NCB nested )
  {
    Objects.requireNonNull(nested);
    this.nested = nested;
  }


  @Override
  public void failed( Exception ex )
  {
    nested.failed(ex);
  }


  @Override
  public void cancelled()
  {
    nested.cancelled();
  }


  public static <T, N, NCB extends FutureCallback<? super N>>
  NestedFutureCallback<T, N, NCB> getInstance( NCB _nested,
    final BiConsumer<? super T, ? super NCB> completionCallback )
  {
    Objects.requireNonNull(completionCallback, "completion callback");
    return new NestedFutureCallback<T, N, NCB>(_nested)
      {
        @Override
        public void completed( T result )
        {
          completionCallback.accept(result, nested);
        }
      };
  }
}
