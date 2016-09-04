package kaleidok.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kaleidok.http.responsehandler.JsonElementResponseHandler;
import kaleidok.http.responsehandler.JsonMimeTypeChecker;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.http.util.URLEncoding;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;


public class JsonHttpConnection extends HttpConnection
{
  private volatile Object json;


  public JsonHttpConnection( HttpURLConnection c )
  {
    super(c, JsonMimeTypeChecker.MIME_TYPE_MAP);
    defaultCharset = URLEncoding.DEFAULT_CHARSET;
    setDoInput(true);
  }


  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static JsonHttpConnection openURL( URL url ) throws IOException
  {
    try {
      return openURL(url, JsonHttpConnection.class);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }


  public JsonElement get() throws IOException
  {
    if (json != null) {
      synchronized (this) {
        if (json == null) {
          try (Reader reader = getReader()) {
            JsonElement json =
              JsonElementResponseHandler.getDefaultJsonParser().parse(reader);
            this.json = json;
            return json;
          } finally {
            disconnect();
          }
        }
      }
    }
    if (json instanceof JsonElement)
      return (JsonElement) json;
    throw getPreviouslyParsedException();
  }


  public <T> T get( Class<T> clazz ) throws IOException
  {
    return get(clazz, TypeAdapterManager.getGson());
  }


  public <T> T get( Class<T> clazz, Gson gson ) throws IOException
  {
    if (json == null) {
      synchronized (this) {
        if (json == null) {
          try (Reader reader = getReader()) {
            T json = gson.fromJson(reader, clazz);
            this.json = json;
            return json;
          } finally {
            disconnect();
          }
        }
      }
    }
    if (!clazz.isInstance(json))
      throw getPreviouslyParsedException();
    //noinspection unchecked
    return (T) json;
  }


  private IllegalStateException getPreviouslyParsedException()
  {
    return new IllegalStateException(
      "Already parsed stream to " + json.getClass().getCanonicalName());
  }


  public Callable<JsonElement> asCallable()
  {
    return this::get;
  }


  public <T> Callable<T> asCallable( Class<T> clazz )
  {
    return asCallable(clazz, TypeAdapterManager.getGson());
  }


  public <T> Callable<T> asCallable( final Class<T> clazz, final Gson gson )
  {
    return () -> get(clazz, gson);
  }
}
