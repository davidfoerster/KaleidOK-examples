package kaleidok.http.util;

import kaleidok.util.StringTokenIterator;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static kaleidok.http.URLEncoding.decode;


public final class Parsers
{
  protected static final String CHARSET = "charset";

  private Parsers() { }


  public static Map<String, String> getQueryMap( URI uri, Charset chs )
  {
    String q = uri.getRawQuery();
    if (q == null)
      return null;

    Map<String, String> qm = new HashMap<>(8);
    for (String strParam : new StringTokenIterator(q, '&')) {
      if (!strParam.isEmpty()) {
        StringTokenIterator itParam = new StringTokenIterator(strParam, '=');
        String name = decode(itParam.next(), chs),
          value = itParam.hasNext() ?
            decode(strParam.substring(itParam.getBegin()), chs) :
            null;
        if (qm.containsKey(name))
          throw new IllegalArgumentException("Query parameter appears multiple times: " + name);
        qm.put(name, value);
      }
    }
    return qm;
  }

  public static Map<String, String> getHeaderValueMap( String h )
  {
    Map<String, String> hm = new HashMap<>(4);
    for (String strParam : h.split("\\s*;\\s*", 0)) {
      if (!strParam.isEmpty()) {
        StringTokenIterator itParam = new StringTokenIterator(strParam, '=');
        String name = itParam.next(),
          value = itParam.hasNext() ? strParam.substring(itParam.getBegin()) : null;
        if (value == null) {
          value = name;
          name = null;
        }
        if (hm.containsKey(name))
          throw new IllegalArgumentException("Header parameter appears multiple times: " + name);
        hm.put(name, value);
      }
    }
    return hm;
  }


  public static class ContentType
  {
    public static final ContentType EMPTY = new ContentType();

    public String mimeType;
    public Charset charset;
  }


  /**
   * Parses and returns the content type of an HTTP response.
   *
   * @param con An HTTP connection object
   * @return A content type object holding the MIME type and the charset
   *    specified in the HTTP response
   *
   * @throws IllegalCharsetNameException see {@link Charset#forName(String)}
   * @throws IllegalArgumentException see {@link Charset#forName(String)}
   * @throws UnsupportedCharsetException see {@link Charset#forName(String)}
   */
  public static ContentType getContentType( HttpURLConnection con )
  {
    String s = con.getContentType();
    if (s == null)
      return null;

    ContentType ct = new ContentType();
    int delimPos = s.indexOf(';');
    ct.mimeType = (delimPos >= 0) ? s.substring(0, delimPos) : s;

    while (delimPos >= 0) {
      int offset = delimPos + 1;
      while (offset < s.length() && s.charAt(offset) == ' ')
        offset++;
      if (offset >= s.length())
        break;

      delimPos = s.indexOf(';', offset);

      if (s.startsWith(CHARSET, offset) &&
        s.charAt(offset + CHARSET.length()) == '=') {
          ct.charset = Charset.forName(
            s.substring(offset + CHARSET.length() + 1,
              (delimPos >= 0) ? delimPos : s.length()));
      }
    }

    return ct;
  }

  public static ContentType getContentType( HttpResponse response )
    throws ClientProtocolException
  {
    HeaderElement[] headerElements =
      response.getEntity().getContentType().getElements();
    switch (headerElements.length) {
    case 0:
      return null;

    case 1:
      HeaderElement he = headerElements[0];
      ContentType ct = new ContentType();
      ct.mimeType = he.getName();

      NameValuePair charsetParam = he.getParameterByName("charset");
      if (charsetParam != null) {
        try {
          ct.charset = Charset.forName(charsetParam.getValue());
        } catch (IllegalArgumentException ex) {
          throw new ClientProtocolException(ex);
        }
      }
      return ct;

    default:
      throw new ClientProtocolException("Multiple content type headers");
    }
  }


  public static InputStream decompressStream( InputStream in,
    String contentEncoding )
    throws IOException
  {
    Class<? extends FilterInputStream> dec = decoders.get(contentEncoding);
    if (dec != null) {
      try {
        in = dec.getConstructor(InputStream.class).newInstance(in);
      } catch (InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof IOException)
          throw (IOException) cause;
        throw new Error(cause); // FilterInputStream constructors shouldn't throw anything except IOException
      } catch (ReflectiveOperationException ex) {
        throw new Error(ex);
      }
    } else if (!decoders.containsKey(null)) {
      throw new ClientProtocolException(
        "Invalid or unsupported content-encoding: " + contentEncoding);
    }
    return in;
  }

  private static final Map<String, Class<? extends FilterInputStream>> decoders =
    new HashMap<String, Class<? extends FilterInputStream>>() {{
      put(null, null);
      put("deflate", InflaterInputStream.class);
      put("gzip", GZIPInputStream.class);
    }};
}
