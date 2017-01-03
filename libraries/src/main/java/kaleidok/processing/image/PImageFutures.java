package kaleidok.processing.image;

import kaleidok.util.concurrent.ImmediateFuture;
import processing.core.PImage;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import static kaleidok.processing.image.PImages.IMAGE_TYPE_PREFERENCE_ORDER;


public final class PImageFutures
{
  private PImageFutures() { }


  public static RunnableFuture<PImage> from( PImage img )
  {
    return ImmediateFuture.of(img);
  }


  public static RunnableFuture<PImage> from( final Image img )
  {
    return
      (img instanceof BufferedImage &&
       ((BufferedImage) img).getType() == IMAGE_TYPE_PREFERENCE_ORDER[0])
      ?
        ImmediateFuture.of(new PImage(img)) :
        new FutureTask<>(() -> PImages.from(img));
  }


  public static RunnableFuture<PImage> from( File f )
  {
    return new CallablePImageFileReader(f).asFuture();
  }


  public static RunnableFuture<PImage> from( URL url )
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
