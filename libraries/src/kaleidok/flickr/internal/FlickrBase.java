package kaleidok.flickr.internal;

import kaleidok.flickr.Size;
import kaleidok.flickr.SizeMap;
import kaleidok.google.gson.TypeAdapterManager;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;


public class FlickrBase
{
  public static final URI DEFAULT_URI_BASE;
  static {
    try {
      DEFAULT_URI_BASE =
        new URI("https", "api.flickr.com", "/services/rest/", null);
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }

    TypeAdapterManager.registerTypeAdapter(
      SizeMap.class, SizeMap.Deserializer.INSTANCE);
    TypeAdapterManager.registerTypeAdapter(
      Size.Label.class, Size.Label.Deserializer.INSTANCE);
  }


  private URI uriBase;

  private String apiKey, apiSecret;

  protected URIBuilder ub;


  public FlickrBase()
  {
    initQueryBase(DEFAULT_URI_BASE, null, null);
  }


  public URI getUriBase()
  {
    return uriBase;
  }


  public void setUriBase( URI uriBase )
  {
    initQuery(uriBase, null, null);
  }


  public void setApiKey( String key, String secret )
  {
    initQuery(null, key, secret);
  }

  public String getApiKey()
  {
    return apiKey;
  }

  public String getApiSecret()
  {
    return apiSecret;
  }


  protected static final String QUERY_API_KEY = "api_key";

  protected void initQuery( URI base, String apiKey, String apiSecret )
  {
    initQueryBase(base, apiKey, apiSecret);
  }

  private void initQueryBase( URI base, String apiKey, String apiSecret )
  {
    if (ub == null)
    {
      //noinspection SpellCheckingInspection
      ub = new URIBuilder()
        .setParameter("format", "json")
        .setParameter("nojsoncallback", "1");
    }

    if (base != null && !base.equals(this.uriBase))
    {
      assert base.getScheme() != null && base.getHost() != null;
      this.uriBase = base;
      ub.setScheme(base.getScheme())
        .setUserInfo(base.getUserInfo())
        .setHost(base.getHost())
        .setPort(base.getPort())
        .setPath(base.getPath());
    }

    if (apiKey == null) {
      apiKey = this.apiKey;
      //apiSecret = this.apiSecret; // currently unused
    } else if (apiSecret != null) {
      this.apiKey = apiKey;
      this.apiSecret = apiSecret;
    } else {
      throw new NullPointerException(
        "apiSecret mustn't be null when apiKey is set");
    }
    if (apiKey != null)
      ub.setParameter(QUERY_API_KEY, apiKey);
  }


  protected URIBuilder copyUriBuilder()
  {
    assert ub.getScheme() != null && ub.getHost() != null;
    return new URIBuilder()
      .setScheme(ub.getScheme())
      .setUserInfo(ub.getUserInfo())
      .setHost(ub.getHost())
      .setPort(ub.getPort())
      .setPath(ub.getPath())
      .setParameters(ub.getQueryParams());
  }


  protected URI getPhotoSizesUri( String photoId )
  {
    try {
      return copyUriBuilder()
        .addParameter("method", "flickr.photos.getSizes")
        .addParameter("photo_id", photoId)
        .build();
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }
}
