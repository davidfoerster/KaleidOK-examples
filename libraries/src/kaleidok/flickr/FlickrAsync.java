package kaleidok.flickr;

import com.google.gson.JsonParseException;
import kaleidok.util.concurrent.NestedFutureCallback;
import kaleidok.http.async.JsonAsync;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;


public class FlickrAsync extends Flickr
{
  private final JsonAsync async;


  public FlickrAsync( JsonAsync async )
  {
    this.async = async;
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
      this.lastCallback = lcb = new FlickrFutureCallback<>(nested);
    }
    //noinspection unchecked
    return (FlickrFutureCallback<T>) lcb;
  }
}
