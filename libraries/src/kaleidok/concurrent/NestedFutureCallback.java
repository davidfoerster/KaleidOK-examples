package kaleidok.concurrent;

import org.apache.http.concurrent.FutureCallback;

import java.util.Objects;


public abstract class NestedFutureCallback<T, N> implements FutureCallback<T>
{
  public final FutureCallback<N> nested;


  protected NestedFutureCallback( FutureCallback<N> nested )
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
}
