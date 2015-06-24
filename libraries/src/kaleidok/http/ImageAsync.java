package kaleidok.http;

import kaleidok.awt.ImageTracker;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.Future;


public class ImageAsync extends AsyncBase
{
  public ImageAsync()
  {
    super(IMAGE_MIMETYPE_MAP);
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


  public static class ImageResponseHandler implements
    ResponseHandler<Image>
  {
    public static final ImageResponseHandler INSTANCE =
      new ImageResponseHandler();

    protected ImageResponseHandler() { }

    @Override
    public Image handleResponse( HttpResponse httpResponse )
      throws IOException
    {
      httpResponse = imageMimeTypeChecker.handleResponse(httpResponse);
      Image img = ImageTracker.loadImage(
        EntityUtils.toByteArray(httpResponse.getEntity()));
      if (img != null)
        return img;
      throw new IOException("Loading image failed");
    }
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


  public static final MimeTypeMap IMAGE_MIMETYPE_MAP =
    new MimeTypeMap() {{
      for (String mimeType: ImageIO.getReaderMIMETypes())
        put(mimeType, null);
      freeze();
    }};

  protected static final ResponseMimeTypeChecker imageMimeTypeChecker =
    new ResponseMimeTypeChecker(IMAGE_MIMETYPE_MAP);
}
