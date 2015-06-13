package kaleidok.flickr;

import com.flickr4java.flickr.photos.Size;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.*;


public class Photo extends com.flickr4java.flickr.photos.Photo
{
  public Photo() { }

  public Photo( CharSequence url )
  {
    fromUrl(url);
  }

  public static final Pattern URL_PATTERN = Pattern.compile(
    "^https?://farm(?<farm>\\d+)\\.static\\.?flickr\\.com/" +
    "(?<server>\\d+)/(?<photoId>\\d+)_(?<secret>\\p{XDigit}+)" +
    "(?:_\\p{Alpha})?\\.(?:jpg|png|gif)$");

  public boolean fromUrl( CharSequence url )
  {
    Matcher m = URL_PATTERN.matcher(url);
    if (m.matches()) {
      setFarm(m.group("farm"));
      setServer(m.group("server"));
      setId(m.group("photoId"));
      setSecret(m.group("secret"));
    }
    return m.matches();
  }

  private Collection<Size> sizes;

  public Collection<Size> getSizes()
  {
    return sizes;
  }

  @Override
  public void setSizes( Collection<Size> sizes )
  {
    this.sizes = sizes;
    super.setSizes(sizes);
  }

  public Size getSize( int sizeId )
  {
    Collection<Size> sizes = getSizes();
    if (sizes != null) {
      for (Size size : sizes) {
        if (size.getLabel() == sizeId)
          return size;
      }
    }
    return null;
  }

  public Size getLargestImageSize()
  {
    Collection<Size> sizes = getSizes();
    return (sizes != null && !sizes.isEmpty()) ?
      Collections.max(sizes, PhotoSizeComparator.INSTANCE) :
      null;
  }
}
