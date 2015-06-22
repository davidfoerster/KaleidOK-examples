package kaleidok.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
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
      Toolkit tk = Toolkit.getDefaultToolkit();
      Image img = tk.createImage(EntityUtils.toByteArray(httpResponse.getEntity()));

      int infoFlags;
      if (tk.prepareImage(img, -1, -1, null)) {
        infoFlags = tk.checkImage(img, -1, -1, null);
      } else {
        ImageReadyObserver obs = new ImageReadyObserver();
        synchronized (obs) {
          infoFlags = tk.checkImage(img, -1, -1, obs);
          if ((infoFlags & ImageReadyObserver.DONE) == 0) {
            while (true) {
              try {
                obs.wait();
                infoFlags = obs.getInfoFlags();
                break;
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }

      if ((infoFlags & ImageReadyObserver.SUCCESS) == 0) {
        throw new IOException("Loading image failed");
      }

      return img;
    }
  }


  private static class ImageReadyObserver implements ImageObserver
  {
    public static final int
      SUCCESS = ALLBITS | FRAMEBITS,
      DONE = SUCCESS | ERROR | ABORT;

    private volatile int infoFlags = 0;

    public int getInfoFlags()
    {
      return infoFlags;
    }

    @Override
    public boolean imageUpdate( Image img, int infoFlags, int x, int y,
      int width, int height )
    {
      this.infoFlags = infoFlags;
      if ((infoFlags & DONE) != 0) {
        synchronized (this) {
          notifyAll();
        }
        return false;
      }
      return true;
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
