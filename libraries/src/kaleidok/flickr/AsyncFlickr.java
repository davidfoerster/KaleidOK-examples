package kaleidok.flickr;

import com.google.gson.JsonParseException;
import kaleidok.concurrent.NestedFutureCallback;
import kaleidok.http.JsonAsync;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;


public class AsyncFlickr extends Flickr
{
  private final JsonAsync async;


  public AsyncFlickr( String apiKey, String apiSecret )
  {
    this(apiKey, apiSecret, null, null);
  }


  public AsyncFlickr( String apiKey, String apiSecret, Executor executor,
    org.apache.http.client.fluent.Executor httpExecutor )
  {
    super(apiKey, apiSecret);

    async = new JsonAsync().use(executor).use(httpExecutor);
    async.gson = getGson();
  }


  protected static class FlickrFutureCallback<T>
    extends NestedFutureCallback<T, T>
  {
    public FlickrFutureCallback( FutureCallback<T> nested )
    {
      super(nested);
    }

    @Override
    public void completed( T t )
    {
      nested.completed(t);
    }

    @Override
    public void failed( Exception ex )
    {
      if (ex instanceof JsonParseException) {
        failed((JsonParseException) ex);
      } else {
        super.failed(ex);
      }
    }

    private void failed( JsonParseException ex )
    {
      Throwable cause = ex.getCause();
      if (cause instanceof FlickrException) {
        failed((FlickrException) cause);
      } else {
        super.failed(ex);
      }
    }

    private void failed( FlickrException ex )
    {
      super.failed(ex);
    }
  }


  public Future<SizeMap> getPhotoSizes( String photoId,
    FutureCallback<SizeMap> callback )
  {
    return async.execute(
      Request.Get(getPhotoSizesUri(photoId)),
      SizeMap.class, getFlickrCallback(callback));
  }


  private FlickrFutureCallback<?> lastCallback = null;

  private <T> FlickrFutureCallback<T> getFlickrCallback(
    FutureCallback<T> nested )
  {
    FlickrFutureCallback<?> lcb = this.lastCallback;
    if (lcb == null || lcb.nested != nested) {
      lcb = new FlickrFutureCallback<>(nested);
      this.lastCallback = lcb;
    }
    return (FlickrFutureCallback<T>) lcb;
  }
}
