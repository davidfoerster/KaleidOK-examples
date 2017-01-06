package kaleidok.http.responsehandler;


import kaleidok.processing.image.PImages;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import java.io.IOException;


public class PImageBaseResponseHandler extends ImageResponseHandler
{
  public static final PImageBaseResponseHandler INSTANCE =
    new PImageBaseResponseHandler();


  protected PImageBaseResponseHandler() { }


  @Override
  protected ImageReadParam getReadParam( ImageReader r, int imageIndex )
    throws IOException
  {
    ImageTypeSpecifier type =
      PImages.getPreferredType(r.getImageTypes(imageIndex));
    if (type != null)
    {
      ImageReadParam param = r.getDefaultReadParam();
      param.setDestinationType(type);
      return param;
    }
    return null;
  }
}
