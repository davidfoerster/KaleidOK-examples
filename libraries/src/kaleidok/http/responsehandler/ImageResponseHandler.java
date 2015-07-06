package kaleidok.http.responsehandler;

import kaleidok.awt.ImageTracker;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.awt.*;
import java.io.IOException;


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
    Image img = ImageTracker.loadImage(
      EntityUtils.toByteArray(httpResponse.getEntity()));
    if (img != null)
      return img;
    throw new IOException("Loading image failed");
  }
}
