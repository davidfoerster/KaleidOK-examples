package kaleidok.flickr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.http.JsonHttpConnection;
import kaleidok.javafx.beans.property.AspectedObjectProperty;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringConversionPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.util.converter.UriStringConverter;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.stream.Stream;


public class Flickr implements PreferenceBean
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
      SizeMap.class, SizeMap::deserialize);
    TypeAdapterManager.registerTypeAdapter(
      Size.Label.class, Size.Label::deserialize);
  }


  private final AspectedObjectProperty<URI> uriBase;

  private final AspectedStringProperty apiKey, apiSecret;

  private final AuthenticationUriBinding authenticationUri;


  public Flickr()
  {
    uriBase =
      new AspectedObjectProperty<>(this, "API base URI", DEFAULT_URI_BASE);
    uriBase.addAspect(StringConverterAspectTag.getInstance(),
      UriStringConverter.INSTANCE);
    uriBase.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringConversionPropertyPreferencesAdapter<>(
        uriBase, Flickr.class, UriStringConverter.INSTANCE));

    apiKey = new AspectedStringProperty(this, "access key");
    apiKey.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringPropertyPreferencesAdapter<>(apiKey, Flickr.class));

    apiSecret = new AspectedStringProperty(this, "access secret");
    apiSecret.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringPropertyPreferencesAdapter<>(apiSecret, Flickr.class));

    authenticationUri = new AuthenticationUriBinding();
  }


  public ObjectProperty<URI> uriBaseProperty()
  {
    return uriBase;
  }

  public URI getUriBase()
  {
    return uriBase.get();
  }

  public void setUriBase( URI uriBase )
  {
    this.uriBase.set(Objects.requireNonNull(uriBase));
  }


  public void setApiKey( String key, String secret )
  {
    if ((key == null) != (secret == null))
    {
      throw new IllegalArgumentException(
        "Key and secret must either be both null or both non-null");
    }

    this.apiKey.set(key);
    this.apiSecret.set(secret);
  }


  public ReadOnlyStringProperty apiKeyProperty()
  {
    return apiKey.getReadOnlyProperty();
  }

  public String getApiKey()
  {
    return apiKey.get();
  }


  public ReadOnlyStringProperty apiSecretProperty()
  {
    return apiSecret.getReadOnlyProperty();
  }

  public String getApiSecret()
  {
    return apiSecret.get();
  }


  protected URI getPhotoSizesUri( String photoId )
  {
    try {
      return authenticationUri.getCopy()
        .addParameter("method", "flickr.photos.getSizes")
        .addParameter("photo_id", photoId)
        .build();
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


  protected URL getPhotoSizeUrl( String photoId )
  {
    try
    {
      return getPhotoSizesUri(photoId).toURL();
    }
    catch (MalformedURLException ex)
    {
      throw new AssertionError(ex);
    }
  }


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


  static JsonObject unwrap( JsonElement jsonElement, String which )
  {
    if (!jsonElement.isJsonObject())
      throw new JsonParseException("JSON object expected");
    JsonObject o = jsonElement.getAsJsonObject();

    String stat = o.getAsJsonPrimitive("stat").getAsString();
    switch (stat) {
    case "ok":
      return o.getAsJsonObject(which);

    case "fail":
      throw new JsonParseException(new FlickrException(
        o.getAsJsonPrimitive("message").getAsString(),
        o.getAsJsonPrimitive("code").getAsInt(), null));

    default:
      throw new JsonParseException(
        "Invalid status code: " + stat);
    }
  }


  @Override
  public String getName()
  {
    return "Flickr";
  }


  @Override
  public Object getParent()
  {
    return null;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.of(
      uriBase.getAspect(PropertyPreferencesAdapterTag.getWritableInstance()),
      apiKey.getAspect(PropertyPreferencesAdapterTag.getWritableInstance()),
      apiSecret.getAspect(PropertyPreferencesAdapterTag.getWritableInstance()));
  }


  private class AuthenticationUriBinding extends ObjectBinding<URIBuilder>
  {
    {
      bind(uriBaseProperty(), apiKeyProperty());
    }


    @Override
    protected URIBuilder computeValue()
    {
      //noinspection SpellCheckingInspection
      URIBuilder ub = new URIBuilder()
        .setParameter("format", "json")
        .setParameter("nojsoncallback", "1");

      URI uriBase = getUriBase();
      ub.setScheme(uriBase.getScheme())
        .setUserInfo(uriBase.getUserInfo())
        .setHost(uriBase.getHost())
        .setPort(uriBase.getPort())
        .setPath(uriBase.getPath());

      String apiKey = getApiKey();
      if (apiKey != null)
        ub.setParameter("api_key", apiKey);

      return ub;
    }


    public URIBuilder getCopy()
    {
      URIBuilder ub = get();
      return new URIBuilder()
        .setScheme(ub.getScheme())
        .setUserInfo(ub.getUserInfo())
        .setHost(ub.getHost())
        .setPort(ub.getPort())
        .setPath(ub.getPath())
        .setParameters(ub.getQueryParams());
    }


    @Override
    public void dispose()
    {
      unbind(uriBaseProperty(), apiKeyProperty());
    }
  }
}
