package kaleidok.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;


public class JsonHttpConnection extends HttpConnection
{
  protected Object json;

  public JsonHttpConnection( HttpURLConnection c )
  {
    super(c, MIME_TYPE_MAP);
    defaultCharset = URLEncoding.DEFAULT_CHARSET;
    setDoInput(true);
  }

  public static JsonHttpConnection openURL( URL url ) throws IOException
  {
    try {
      return (JsonHttpConnection) openURL(url, JsonHttpConnection.class);
    } catch (ReflectiveOperationException e) {
      throw new Error(e);
    }
  }

  public JsonElement get() throws IOException
  {
    if (json != null) {
      synchronized (this) {
        if (json == null) {
          try (Reader reader = getReader()) {
            JsonElement json = getJsonParser().parse(reader);
            this.json = json;
            return json;
          } finally {
            disconnect();
          }
        }
      }
    }
    if (!(json instanceof JsonElement))
      throw getPreviouslyParsedException();
    return (JsonElement) json;
  }

  public <T> T get( Class<T> clazz ) throws IOException
  {
    return get(clazz, getGson());
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
    if (!clazz.isAssignableFrom(json.getClass()))
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
    return new Callable<JsonElement>()
      {
        @Override
        public JsonElement call() throws IOException
        {
          return get();
        }
      };
  }

  public <T> Callable<T> asCallable( Class<T> clazz )
  {
    return asCallable(clazz, getGson());
  }

  public <T> Callable<T> asCallable( final Class<T> clazz, final Gson gson )
  {
    return new Callable<T>()
      {
        @Override
        public T call() throws IOException
        {
          return get(clazz, gson);
        }
      };
  }


  private static JsonParser JSON_PARSER = null;

  public static JsonParser getJsonParser()
  {
    if (JSON_PARSER == null)
      JSON_PARSER = new JsonParser();
    return JSON_PARSER;
  }

  private static Gson gson = null;

  public static Gson getGson()
  {
    if (gson == null)
      gson = new Gson();
    return gson;
  }

  public static final MimeTypeMap MIME_TYPE_MAP = new MimeTypeMap() {{
    put("application/json", MimeTypeMap.ONE);
    put("text/json", 0.9f);
    put("text/javascript", 0.5f);
    put(MimeTypeMap.WILDCARD, 0.1f);
    freeze();
  }};
}
