package kaleidok.processing.image;

import processing.core.PImage;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.RunnableFuture;

import static kaleidok.processing.image.PImages.IMAGE_TYPE_PREFERENCE_ORDER;


public interface PImageFuture
  extends RunnableFuture<PImage>
{
  PImageFuture EMPTY = new ImmediatePImageFuture(null);


  static PImageFuture from( PImage img )
  {
    return (img != null) ? new ImmediatePImageFuture(img) : EMPTY;
  }


  static PImageFuture from( final Image img )
  {
    return
      (img instanceof BufferedImage &&
       ((BufferedImage) img).getType() == IMAGE_TYPE_PREFERENCE_ORDER[0])
      ?
        new ImmediatePImageFuture(new PImage(img)) :
        new SimplePImageFutureTask(() -> PImages.from(img), img);
  }


  static PImageFuture from( File f )
  {
    return new CallablePImageFileReader(f).asFuture();
  }


  static PImageFuture from( URL url )
  {
    if ("file".equals(url.getProtocol())) try
    {
      return from(new File(url.toURI()));
    }
    catch (URISyntaxException ignored)
    {
      // fall back to generic URL handler
    }

    return new CallablePImageUrlReader(url).asFuture();
  }
}
