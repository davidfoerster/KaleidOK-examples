package kaleidok.net.http.responsehandler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


public class ImageResponseHandler implements
  ResponseHandler<Image>
{
  public static final ImageResponseHandler INSTANCE =
    new ImageResponseHandler();


  protected ImageResponseHandler() { }


  @Override
  public Image handleResponse( HttpResponse httpResponse )
    throws IOException
  {
    httpResponse = ImageMimeTypeChecker.INSTANCE.handleResponse(httpResponse);
    return doHandleResponse(httpResponse);
  }


  protected Image doHandleResponse( HttpResponse httpResponse )
    throws IOException
  {
    HttpEntity entity = httpResponse.getEntity();
    ImageReader r = getImageReader(entity);
    try (InputStream is = entity.getContent())
    {
      try (ImageInputStream iis = ImageIO.createImageInputStream(is))
      {
        return readImage(r, iis);
      }
    }
    finally
    {
      r.dispose();
    }
  }


  @SuppressWarnings("MethodMayBeStatic")
  protected ImageReader getImageReader( HttpEntity entity )
  {
    String mimeType = entity.getContentType().getElements()[0].getName();
    Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(mimeType);
    if (it.hasNext())
      return it.next();

    /*
     * This should never happen because the response MIME type has been
     * checked against the list of supported image types.
     */
    throw new InternalError("Unsupported image type: " + mimeType);
  }


  protected Image readImage( ImageReader r, ImageInputStream iis )
    throws IOException
  {
    r.setInput(iis, true, true);
    int imageIndex = Math.max(r.getNumImages(false) - 1, 0);
    return r.read(imageIndex, getReadParam(r, imageIndex));
  }


  protected ImageReadParam getReadParam( ImageReader r, int imageIndex )
    throws IOException
  {
    return null;
  }
}
