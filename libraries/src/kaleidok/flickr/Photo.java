package kaleidok.flickr;

import java.io.IOException;
import java.util.regex.*;


public abstract class Photo
{
  public String farm, server, id, secret, extension;

  private SizeMap sizes;


  public Photo() { }

  public Photo( CharSequence url )
  {
    fromUrl(url);
  }


  public static final Pattern URL_PATTERN = Pattern.compile(
    "^https?://farm(?<farm>\\d+)\\.static\\.?flickr\\.com/" +
    "(?<server>\\d+)/(?<photoId>\\d+)_(?<secret>\\p{XDigit}+)" +
    "(?:_\\p{Alpha})?\\.(?<ext>jpg|png|gif)$");

  public boolean fromUrl( CharSequence url )
  {
    Matcher m = URL_PATTERN.matcher(url);
    if (m.matches()) {
      farm = m.group("farm");
      server = m.group("server");
      id = m.group("photoId");
      secret = m.group("secret");
      extension = m.group("ext");
    }
    return m.matches();
  }


  public String getMediumUrl()
  {
    return "https://farm" + farm + ".static.flickr.com/" +
      server + '/' + id + '_' + secret + '.' + extension;
  }


  public SizeMap getSizes()
  {
    return sizes;
  }

  public void setSizes( SizeMap sizes )
  {
    this.sizes = sizes;
  }


  public Size getSize( Size.Label label )
  {
    SizeMap sizes = getSizes();
    return (sizes != null) ? sizes.get(label) : null;
  }

  public Size getLargestImageSize()
  {
    SizeMap sizes = getSizes();
    return (sizes != null && !sizes.isEmpty()) ?
      sizes.lastEntry().getValue() :
      null;
  }

  public abstract SizeMap getSizesThrow() throws FlickrException, IOException;
}
