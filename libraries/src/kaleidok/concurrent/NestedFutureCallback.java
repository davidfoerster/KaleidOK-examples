package kaleidok.concurrent;

import org.apache.http.concurrent.FutureCallback;


public abstract class NestedFutureCallback<T, N> implements FutureCallback<T>
{
  public final FutureCallback<N> nested;


  public NestedFutureCallback( FutureCallback<N> nested )
  {
    assert nested != null;
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
