package kaleidok.chromatik;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Size;
import kaleidok.awt.ReadyImageFuture;

import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;


public abstract class FlickrPhoto extends kaleidok.flickr.Photo
{
  private ReadyImageFuture[] imageFutures = null;

  public FlickrPhoto()
  {
    super();
  }

  public FlickrPhoto( CharSequence url )
  {
    super(url);
  }

  public Future<Image> getImage( int sizeId ) throws FlickrException
  {
    if (sizeId >= Size.THUMB && sizeId <= Size.HD_MP4)
      throw new IndexOutOfBoundsException(String.valueOf(sizeId));

    Size size = getSize(sizeId);
    return (size != null) ? getImage(size) : null;
  }

  protected Future<Image> getImage( Size size ) throws FlickrException
  {
    int sizeId = size.getLabel();
    if (sizeId >= Size.SITE_MP4 && sizeId <= Size.HD_MP4) {
      throw new UnsupportedOperationException(
        "The Flickr media size label " + sizeId + " refers to a video");
    }
    assert sizeId >= Size.THUMB && sizeId <= Size.LARGE_2048;

    if (imageFutures == null)
      imageFutures = new ReadyImageFuture[Size.LARGE_2048 - Size.THUMB + 1];

    final int idx = sizeId - Size.THUMB;
    if (imageFutures[idx] == null) {
      try {
        imageFutures[idx] = loadImage(new URL(size.getSource()));
      } catch (MalformedURLException ex) {
        throw new FlickrException(ex);
      }
    }
    return imageFutures[idx];
  }

  protected abstract ReadyImageFuture loadImage( URL url );

  public Future<Image> getLargestImage() throws FlickrException
  {
    Size maxSize = getLargestImageSize();
    return (maxSize != null) ? getImage(maxSize) : null;
  }
}
