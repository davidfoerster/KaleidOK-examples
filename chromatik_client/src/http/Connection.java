package http;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;


public class Connection
{
  public static final String HTTP_PROTOCOL = "http";

  public final HttpURLConnection c;

  public MimeTypeMap acceptedMimeTypes;

  protected String responseMimeType;

  protected Charset responseCharset;

  protected InputStream inputStream;

  protected Reader reader;

  public Connection( HttpURLConnection c )
  {
    this(c, new MimeTypeMap());
  }

  public Connection( HttpURLConnection c, MimeTypeMap acceptedMimeTypes )
  {
    this.c = c;
    this.acceptedMimeTypes = acceptedMimeTypes;
  }

  public static Connection openURL( URL url ) throws IOException
  {
    try {
      return openURL(url, Connection.class);
    } catch (ReflectiveOperationException e) {
      throw new Error(e);
    }
  }

  protected static Connection openURL( URL url, Class<? extends Connection> clazz )
    throws IOException, ReflectiveOperationException
  {
    checkHttpProtocol(url);
    Constructor<? extends Connection> ctor =
      clazz.getConstructor(HttpURLConnection.class);
    return ctor.newInstance(url.openConnection());
  }

  private static void checkHttpProtocol( URL url ) throws IOException
  {
    String p = url.getProtocol(), h = HTTP_PROTOCOL;
    int pl = p.length(), hl = h.length();
    if (!(p.startsWith(h) && (pl == hl || (pl == hl + 1 && p.charAt(hl) == 's'))))
      throw new IOException("Unsupported protocol: " + url.getProtocol());
  }

  public void connect() throws IOException
  {
    if (acceptedMimeTypes != null && c.getRequestProperty("Accept") == null) {
      String accept = acceptedMimeTypes.toString();
      if (!accept.isEmpty())
        c.setRequestProperty("Accept", accept);
    }

    c.connect();
  }

  public InputStream getInputStream() throws IOException
  {
    if (inputStream == null) {
      connect();
      if (c.getResponseCode() != HttpURLConnection.HTTP_OK)
        throw new IOException(
          "HTTP server returned status code " + c.getResponseCode());

      if (acceptedMimeTypes != null && !acceptedMimeTypes.isEmpty()) {
        String mimeType = getResponseMimeType();
        if (acceptedMimeTypes.allows(mimeType) == null)
          throw new IOException("Unsupported response MIME type: " + mimeType);
      }

      Charset charset = getResponseCharset();
      if (charset == null)
        throw new IOException("No response responseCharset");

      String contentEncoding = c.getContentEncoding();
      Class<? extends FilterInputStream> dec = decoders.get(contentEncoding);
      if (dec == null) {
        if (!decoders.containsKey(contentEncoding))
          throw new IOException(
            "Unsupported content-encoding: " + contentEncoding);
        inputStream = c.getInputStream();
      } else try {
        Constructor<? extends FilterInputStream> ctor =
          dec.getConstructor(InputStream.class);
        inputStream = ctor.newInstance(c.getInputStream());
      } catch (InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof IOException)
          throw (IOException) cause;
        throw new Error(cause);
      } catch (ReflectiveOperationException ex) {
        throw new Error(ex);
      }
    }
    return inputStream;
  }

  public Reader getReader() throws IOException
  {
    if (reader == null) {
      InputStream in = getInputStream();
      assert responseCharset != null;
      reader = new InputStreamReader(in, responseCharset);
    }
    return reader;
  }

  private void parseContentType() throws IOException
  {
    String ct = c.getContentType();
    if (ct == null)
      return;

    int delimPos = ct.indexOf(';');
    responseMimeType = (delimPos >= 0) ? ct.substring(0, delimPos) : ct;

    while (delimPos >= 0) {
      int offset = delimPos + 1;
      while (ct.charAt(offset) == ' ')
        offset++;

      delimPos = ct.indexOf(';', offset);

      if (ct.startsWith(CHARSET, offset) &&
        ct.charAt(offset + CHARSET.length()) == '=') {
        try {
          responseCharset = Charset.forName(
            ct.substring(offset + CHARSET.length() + 1,
              (delimPos >= 0) ? delimPos : ct.length()));
        } catch (IllegalArgumentException ex) {
          throw new IOException(ex);
        }
      }
    }
  }

  public String getResponseMimeType() throws IOException
  {
    if (responseMimeType == null)
      parseContentType();
    return responseMimeType;
  }

  public Charset getResponseCharset() throws IOException
  {
    if (responseMimeType == null)
      parseContentType();
    return responseCharset;
  }

  public static final Map<String, Class<? extends FilterInputStream>> decoders =
    new HashMap<String, Class<? extends FilterInputStream>>() {{
      put(null, null);
      put("deflate", InflaterInputStream.class);
      put("gzip", GZIPInputStream.class);
    }};

  protected static final String CHARSET = "charset";
}
