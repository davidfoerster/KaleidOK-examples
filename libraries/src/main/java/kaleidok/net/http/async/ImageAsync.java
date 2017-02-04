package kaleidok.net.http.async;

import kaleidok.net.http.responsehandler.ImageMimeTypeChecker;
import kaleidok.net.http.responsehandler.ImageResponseHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.awt.Image;
import java.util.concurrent.Future;


public class ImageAsync extends AsyncBase
{
  protected final ResponseHandler<Image> responseHandler;


  public ImageAsync( org.apache.http.client.fluent.Async fluentAsync,
    ResponseHandler<Image> responseHandler )
  {
    super(fluentAsync, ImageMimeTypeChecker.IMAGE_MIMETYPE_MAP);
    this.responseHandler = responseHandler;
  }


  public ImageAsync( org.apache.http.client.fluent.Async fluentAsync )
  {
    this(fluentAsync, ImageResponseHandler.INSTANCE);
  }


  @Override
  public Future<Image> execute( Request request )
  {
    return execute(request, (FutureCallback<Image>) null);
  }


  public Future<Image> execute( Request request,
    FutureCallback<Image> callback )
  {
    applyAcceptedMimeTypes(request);
    return underlying.execute(request, responseHandler, callback);
  }
}
