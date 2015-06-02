package kaleidok.http;

import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;


public class JsonHttpConnection extends HttpConnection
{
  protected Object json;

  public JsonHttpConnection( HttpURLConnection c )
  {
    super(c, MIME_TYPE_MAP);
    defaultCharset = DEFAULT_CHARSET;
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

  public JSONArray getArray() throws IOException
  {
    if (json == null) {
      json = new JSONArray(getReader());
      reader.close();
    }
    return (json instanceof JSONArray) ? (JSONArray) json : null;
  }

  public JSONObject getObject() throws IOException
  {
    if (json == null) {
      json = new JSONObject(getReader());
      reader.close();
    }
    return (json instanceof JSONObject) ? (JSONObject) json : null;
  }

  public static final MimeTypeMap MIME_TYPE_MAP = new MimeTypeMap() {{
    put("application/json", MimeTypeMap.ONE);
    put("text/json", 0.9f);
    put("text/javascript", 0.5f);
    put(MimeTypeMap.WILDCARD, 0.1f);
    freeze();
  }};

  public static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");
}
