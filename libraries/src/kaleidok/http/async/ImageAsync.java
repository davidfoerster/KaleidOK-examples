package kaleidok.http.async;

import kaleidok.http.responsehandler.ImageMimeTypeChecker;
import kaleidok.http.responsehandler.ImageResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.awt.Image;
import java.util.concurrent.Future;


public class ImageAsync extends AsyncBase
{
  public ImageAsync( org.apache.http.client.fluent.Async fluentAsync )
  {
    super(fluentAsync, ImageMimeTypeChecker.IMAGE_MIMETYPE_MAP);
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
    return underlying.execute(request, ImageResponseHandler.INSTANCE, callback);
  }
}
