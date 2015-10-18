package kaleidok.flickr;


import com.google.gson.JsonParseException;
import kaleidok.flickr.internal.FlickrBase;
import kaleidok.http.JsonHttpConnection;

import java.io.IOException;
import java.net.MalformedURLException;


public class Flickr extends FlickrBase
{
  public Flickr() { }

  public Flickr( String apiKey, String apiSecret )
  {
    super(DEFAULT_URI_BASE, apiKey, apiSecret);
  }


  public SizeMap getPhotoSizes( String photoId )
    throws FlickrException, IOException
  {
    try {
      return JsonHttpConnection.openURL(getPhotoSizesUri(photoId).toURL())
        .get(SizeMap.class);
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    } catch (JsonParseException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof FlickrException)
        throw (FlickrException) cause;
      throw ex;
    }
  }
}
