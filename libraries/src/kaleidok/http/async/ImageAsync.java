package kaleidok.http.async;

import kaleidok.http.responsehandler.ImageMimeTypeChecker;
import kaleidok.http.responsehandler.ImageResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.awt.Image;
import java.util.concurrent.Future;


public class ImageAsync extends AsyncBase
{
  public ImageAsync()
  {
    super(ImageMimeTypeChecker.IMAGE_MIMETYPE_MAP);
  }


  @Override
  public ImageAsync use( Executor executor )
  {
    super.use(executor);
    return this;
  }

  @Override
  public ImageAsync use( java.util.concurrent.Executor concurrentExec )
  {
    super.use(concurrentExec);
    return this;
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
