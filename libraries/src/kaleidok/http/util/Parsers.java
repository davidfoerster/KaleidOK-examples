package kaleidok.http.util;

import kaleidok.util.StringTokenIterator;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static kaleidok.http.URLEncoding.decode;


public final class Parsers
{
  private Parsers() { }


  public static Map<String, String> getQueryMap( URI uri, Charset chs )
  {
    String q = uri.getRawQuery();
    return (q != null) ? getQueryMap(q, chs) : null;
  }


  public static Map<String, String> getQueryMap( String q, Charset chs )
  {
    StringBuilder sb = null;
    Map<String, String> qm = new HashMap<>(8);
    for (String strParam : new StringTokenIterator(q, '&')) {
      if (!strParam.isEmpty()) {
        int p = strParam.indexOf('=');
        CharSequence name, value;
        name = decode((p >= 0) ? strParam.substring(0, p) : strParam, chs, sb);
        if (name instanceof StringBuilder) {
          sb = (StringBuilder) name;
          name = sb.toString();
          sb.setLength(0);
        }
        if (p >= 0) {
          value = decode(strParam.substring(p + 1), chs, sb);
          if (value instanceof StringBuilder) {
            sb = (StringBuilder) value;
            value = sb.toString();
            sb.setLength(0);
          }
        } else {
          value = null;
        }

        String sName = (String) name;
        if (qm.containsKey(sName)) {
          throw new IllegalArgumentException(
            "Query parameter appears multiple times: " + sName);
        }
        qm.put(sName, (String) value);
      }
    }
    return qm;
  }


  /**
   * Parses and returns the content type of an HTTP response.
   *
   * @param con An HTTP connection object
   * @return A content type object holding the MIME type and the charset
   *    specified in the HTTP response
   *
   * @throws ParseException see {@link ContentType#parse(String)}
   * @throws UnsupportedCharsetException see {@link Charset#forName(String)}
   */
  public static ContentType getContentType( HttpURLConnection con )
    throws UnsupportedCharsetException, ParseException
  {
    String s = con.getContentType();
    return (s != null) ? ContentType.parse(s) : null;
  }


  public static ContentType getContentType( HttpResponse response )
  {
    return ContentType.get(response.getEntity());
  }


  public static final ContentType EMPTY_CONTENT_TYPE = ContentType.create(null);


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
        throw new AssertionError(cause); // FilterInputStream constructors shouldn't throw anything except IOException
      } catch (ReflectiveOperationException ex) {
        throw new AssertionError(ex);
      }
    } else if (!decoders.containsKey(contentEncoding)) {
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
