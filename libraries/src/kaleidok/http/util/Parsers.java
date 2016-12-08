package kaleidok.http.util;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;

import java.io.FilterInputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static kaleidok.http.util.URLEncoding.decode;


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
    StringTokenizer tokenizer = new StringTokenizer(q, "&");
    while (tokenizer.hasMoreTokens())
    {
      String strParam = tokenizer.nextToken();
      if (!strParam.isEmpty())
      {
        int p = strParam.indexOf('=');
        CharSequence name, value;

        if (p != 0)
        {
          name = decode((p >= 0) ? strParam.substring(0, p) : strParam, chs, sb);
          if (name instanceof StringBuilder)
          {
            sb = (StringBuilder) name;
            name = sb.toString();
            sb.setLength(0);
          }
        }
        else
        {
          name = "";
        }

        if (p >= 0)
        {
          if (p != strParam.length() - 1)
          {
            value = decode(strParam.substring(p + 1), chs, sb);
            if (value instanceof StringBuilder)
            {
              sb = (StringBuilder) value;
              value = sb.toString();
              sb.setLength(0);
            }
          }
          else
          {
            value = "";
          }
        }
        else
        {
          value = null;
        }

        String sName = (String) name;
        if (qm.containsKey(sName))
        {
          throw new IllegalArgumentException(
            "Query parameter appears multiple times: \"" + sName + '\"');
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
    return (s != null) ? ContentType.parse(s) : EMPTY_CONTENT_TYPE;
  }


  public static ContentType getContentType( HttpResponse response )
  {
    return ContentType.get(response.getEntity());
  }


  public static final ContentType EMPTY_CONTENT_TYPE = ContentType.create(null);


  public static final DecoderMap DECODERS;
  static
  {
    DecoderMap d = DECODERS = new DecoderMap(6);
    d.put(null, (Constructor<? extends FilterInputStream>) null);
    d.put("deflate", InflaterInputStream.class);
    d.put("gzip", GZIPInputStream.class);
    d.freeze();
  }
}
