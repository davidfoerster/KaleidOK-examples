package kaleidok.processing.image;

import org.apache.commons.lang3.ArrayUtils;
import processing.core.PImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.awt.image.BufferedImage.*;
import static org.apache.commons.lang3.ArrayUtils.INDEX_NOT_FOUND;


public final class PImages
{
  private PImages() { }


  static final int[] IMAGE_TYPE_PREFERENCE_ORDER = {
    TYPE_INT_ARGB, TYPE_INT_RGB,
    TYPE_4BYTE_ABGR, TYPE_3BYTE_BGR, // only since Processing 3.0a11
  };


  /**
   * Finds the most suitable available image type for an invocation of
   * {@link PImage#PImage(Image)}, i. e. with the least overhead to copy or
   * convert pixel data.
   *
   * @param it  An iterator of available image types
   * @return  The most suitable image type; or {@code null} if none could be
   *   found
   */
  public static ImageTypeSpecifier getPreferredType(
    Iterator<ImageTypeSpecifier> it )
  {
    int bestPriority = Integer.MAX_VALUE;
    ImageTypeSpecifier bestType = null;

    while (it.hasNext())
    {
      ImageTypeSpecifier type = it.next();
      int priority = ArrayUtils.indexOf(
        IMAGE_TYPE_PREFERENCE_ORDER, type.getBufferedImageType());
      if (priority != INDEX_NOT_FOUND && priority < bestPriority)
      {
        if (priority == 0)
          return type;

        bestPriority = priority;
        bestType = type;
      }
    }

    return bestType;
  }


  /**
   * Constructs an AWT image with the most suitable pixel data format according
   * to the given image reader and source for later conversion to a Processing
   * image object.
   *
   * @param r  An image reader capable to decoding the image source
   * @param iis  An image source
   * @return  An AWT image object suitable for conversion to a Processing image
   *   object
   * @throws IOException on I/O or decoder error
   * @see #getPreferredType(Iterator)
   */
  public static BufferedImage getSuitableImage( ImageReader r,
    ImageInputStream iis )
    throws IOException
  {
    r.setInput(iis, true, true);
    int imageIndex = Math.max(r.getNumImages(false) - 1, 0);

    ImageTypeSpecifier type = getPreferredType(r.getImageTypes(imageIndex));
    ImageReadParam param;
    if (type != null)
    {
      param = r.getDefaultReadParam();
      param.setDestinationType(type);
    }
    else
    {
      param = null;
    }

    return r.read(imageIndex, param);
  }


  public static ImageReader getSuitableReader( ImageInputStream iis,
    String mimeType, String fileExtension )
  {
    ImageReader imageReader;
    try
    {
      if (mimeType != null)
      {
        imageReader = getFirstImageReader(
          ImageIO.getImageReadersByMIMEType(mimeType));
      }
      else
      {
        imageReader =
          (fileExtension != null && !fileExtension.isEmpty()) ?
            getFirstImageReader(
              ImageIO.getImageReadersBySuffix(fileExtension), null) :
            null;
        if (imageReader == null)
          imageReader = getFirstImageReader(ImageIO.getImageReaders(iis));
      }
    }
    catch (NoSuchElementException ex)
    {
      throw new IllegalArgumentException(ex);
    }
    return imageReader;
  }


  public static PImage from( File f )
    throws IOException, IllegalArgumentException
  {
    return new CallablePImageFileReader(f).call();
  }


  public static PImage from( URL url ) throws IOException
  {
    if ("file".equals(url.getProtocol()))
    {
      try
      {
        return from(new File(url.toURI()));
      }
      catch (URISyntaxException ignored)
      {
        // fall back to default method
      }
    }

    return fromUrlImpl(url);
  }


  public static PImage from( URI uri ) throws IOException
  {
    return "file".equals(uri.getScheme()) ?
      from(new File(uri)) :
      fromUrlImpl(uri.toURL());
  }


  private static PImage fromUrlImpl( URL url ) throws IOException
  {
    return new CallablePImageUrlReader(url).call();
  }


  public static PImage from( Image awtImage )
  {
    return new PImage(awtImage);
  }


  static ImageReader getFirstImageReader(
    Iterator<ImageReader> readers, ImageReader defaultReader )
  {
    return readers.hasNext() ? readers.next() : defaultReader;
  }


  static ImageReader getFirstImageReader( Iterator<ImageReader> readers )
    throws NoSuchElementException
  {
    if (readers.hasNext())
      return readers.next();

    throw new NoSuchElementException("No suitable image decoder found");
  }


  public static final int RGB_MASK = (1 << (3 * Byte.SIZE)) - 1;

  @SuppressWarnings("AssignmentToForLoopParameter")
  public static PImage filter( PImage src, PImage dst, RGBImageFilter filter )
  {
    if (filter ==  null)
      throw new NullPointerException("filter");

    src.loadPixels();
    int width = src.width, height = src.height;
    int[] spx = src.pixels;

    if (dst == null)
      dst = new PImage();
    if (src != dst)
    {
      dst.init(width, height, src.format, src.pixelDensity);
      dst.loadPixels();
    }
    int[] dpx = dst.pixels;

    for (int i = 0, y = 0; y < height; y++)
    {
      for (int x = 0; x < width; x++, i++)
      {
        int px = spx[i];
        dpx[i] = filter.filterRGB(x, y, px & RGB_MASK) | (px & ~RGB_MASK);
      }
    }

    dst.updatePixels();
    return dst;
  }
}
