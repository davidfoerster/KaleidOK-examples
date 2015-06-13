package kaleidok.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;


public final class URLEncoding
{
  private URLEncoding() { }

  public static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

  public static String encode( String s )
  {
    try {
      return URLEncoder.encode(s, DEFAULT_CHARSET.name());
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError(ex);
    }
  }

  public static String decode( String s )
  {
    try {
      return URLDecoder.decode(s, DEFAULT_CHARSET.name());
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError(ex);
    }
  }
}
