package kaleidok.flickr;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.*;


public abstract class Photo implements Serializable
{
  private static final long serialVersionUID = 4182367015571487353L;

  public String farm, server, id, secret, extension;

  private SizeMap sizes;


  public Photo() { }

  public Photo( CharSequence url )
  {
    parseUrl(url);
  }


  public static final Pattern URL_PATTERN = Pattern.compile(
    "^https?://farm(?<farm>\\d+)\\.static\\.?flickr\\.com/" +
    "(?<server>\\d+)/(?<photoId>\\d+)_(?<secret>\\p{XDigit}+)" +
    "(?:_\\p{Alpha})?\\.(?<ext>jpg|png|gif)$");

  public void parseUrl( CharSequence url )
  {
    Matcher m = URL_PATTERN.matcher(url);
    if (!m.matches())
      throw new IllegalArgumentException(url.toString());

    farm = m.group("farm");
    server = m.group("server");
    id = m.group("photoId");
    secret = m.group("secret");
    extension = m.group("ext");
  }


  public String getMediumUrl()
  {
    return "https://farm" + farm + ".static.flickr.com/" +
      server + '/' + id + '_' + secret + '.' + extension;
  }


  @Override
  public String toString()
  {
    return getMediumUrl();
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
