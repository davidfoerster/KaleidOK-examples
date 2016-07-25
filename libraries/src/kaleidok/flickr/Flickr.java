package kaleidok.flickr;


import com.google.gson.JsonParseException;
import kaleidok.http.JsonHttpConnection;

import java.io.IOException;
import java.net.MalformedURLException;


public class Flickr extends FlickrBase
{
  public SizeMap getPhotoSizes( String photoId )
    throws FlickrException, IOException
  {
    try (JsonHttpConnection con =
      JsonHttpConnection.openURL(getPhotoSizeUrl(photoId)))
    {
      return con.get(SizeMap.class);
    } catch (JsonParseException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof FlickrException)
        throw (FlickrException) cause;
      throw ex;
    }
  }
}
