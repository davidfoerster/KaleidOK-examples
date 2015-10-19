package kaleidok.http;

import kaleidok.http.util.MimeTypeMap;
import kaleidok.http.util.Parsers;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


public class HttpConnection
{
  public static final String HTTP_PROTOCOL = "http";

  final HttpURLConnection c;

  public MimeTypeMap acceptedMimeTypes;

  public Charset defaultCharset;

  protected ContentType responseContentType;

  protected InputStream inputStream;

  protected Reader reader;

  protected String body;

  public static int
    UNCONNECTED = 0,
    CONNECTED = 1,
    DISCONNECTED = 2;

  private int state = UNCONNECTED;

  public HttpConnection( HttpURLConnection c )
  {
    this(c, new MimeTypeMap());
  }

  public HttpConnection( HttpURLConnection c, MimeTypeMap acceptedMimeTypes )
  {
    this.c = c;
    this.acceptedMimeTypes = acceptedMimeTypes;
  }

  public static HttpConnection openURL( URL url ) throws IOException
  {
    try {
      return openURL(url, HttpConnection.class);
    } catch (ReflectiveOperationException e) {
      throw new Error(e);
    }
  }

  protected static HttpConnection openURL( URL url, Class<? extends HttpConnection> clazz )
    throws IOException, ReflectiveOperationException
  {
    checkHttpProtocol(url);
    Constructor<? extends HttpConnection> ctor =
      clazz.getConstructor(constructorArgumentTypes);
    return ctor.newInstance(url.openConnection());
  }

  private static void checkHttpProtocol( URL url ) throws IOException
  {
    String p = url.getProtocol(), h = HTTP_PROTOCOL;
    int pl = p.length(), hl = h.length();
    if (!(p.startsWith(h) && (pl == hl || (pl == hl + 1 && p.charAt(hl) == 's'))))
      throw new IOException("Unsupported protocol: " + p);
  }

  private static final Class<?>[] constructorArgumentTypes =
    new Class<?>[]{ HttpURLConnection.class };

  /**
   * @see HttpURLConnection#connect()
   */
  public synchronized void connect() throws IOException
  {
    if (state == CONNECTED)
      return;
    if (state != UNCONNECTED)
      throw new IllegalStateException();

    if (acceptedMimeTypes != null) {
      String accept = acceptedMimeTypes.toString();
      if (!accept.isEmpty())
        c.setRequestProperty("Accept", accept);
    }

    c.connect();
    state = CONNECTED;
  }

  /**
   * @see HttpURLConnection#setConnectTimeout(int)
   */
  public void setConnectTimeout( int timeout )
  {
    c.setConnectTimeout(timeout);
  }

  /**
   * @see HttpURLConnection#getConnectTimeout()
   */
  public int getConnectTimeout()
  {
    return c.getConnectTimeout();
  }

  /**
   * @see HttpURLConnection#setReadTimeout(int)
   */
  public void setReadTimeout( int timeout )
  {
    c.setReadTimeout(timeout);
  }

  /**
   * @see HttpURLConnection#getReadTimeout()
   */
  public int getReadTimeout()
  {
    return c.getReadTimeout();
  }

  /**
   * @see HttpURLConnection#getURL()
   */
  public URL getURL()
  {
    return c.getURL();
  }

  /**
   * @see HttpURLConnection#getContentLength()
   */
  public int getContentLength()
  {
    return (getContentEncoding() == null) ? c.getContentLength() : -1;
  }

  /**
   * @see HttpURLConnection#getContentLengthLong()
   */
  public long getContentLengthLong()
  {
    return (getContentEncoding() == null) ? c.getContentLengthLong() : -1;
  }

  /**
   * @see HttpURLConnection#getContentEncoding()
   */
  public String getContentEncoding()
  {
    return c.getContentEncoding();
  }

  /**
   * @see HttpURLConnection#getContentType()
   */
  public String getContentType()
  {
    return c.getContentType();
  }

  /**
   * @see HttpURLConnection#getHeaderField(String)
   */
  public String getHeaderField( String name )
  {
    return c.getHeaderField(name);
  }

  public Map<String, List<String>> getHeaderFields()
  {
    return c.getHeaderFields();
  }

  /**
   * @see HttpURLConnection#getHeaderFieldInt(String, int)
   */
  public int getHeaderFieldInt( String name, int Default )
  {
    return c.getHeaderFieldInt(name, Default);
  }

  /**
   * @see HttpURLConnection#getHeaderFieldLong(String, long)
   */
  public long getHeaderFieldLong( String name, long Default )
  {
    return c.getHeaderFieldLong(name, Default);
  }

  /**
   * @see HttpURLConnection#getInputStream()
   */
  public synchronized InputStream getInputStream() throws IOException
  {
    if (inputStream == null) {
      connect();

      InputStream rawInputStream = null;
      try {
        if (acceptedMimeTypes != null && !acceptedMimeTypes.isEmpty()) {
          String mimeType = getResponseMimeType();
          if (acceptedMimeTypes.allows(mimeType) == null)
            throw new IOException("Unsupported response MIME type: " + mimeType);
        }

        String contentEncoding = c.getContentEncoding();
        if (contentEncoding == null) {
          contentEncoding = c.getHeaderField("transfer-encoding");
          if ("chunked".equals(contentEncoding))
            contentEncoding = null;
        }

        rawInputStream = c.getInputStream();
        inputStream = Parsers.decompressStream(rawInputStream, contentEncoding);

      } finally {
        if (inputStream == null) {
          if (rawInputStream != null)
            rawInputStream.close();
          disconnect();
        }
      }
    }
    return inputStream;
  }

  /**
   * @see HttpURLConnection#getOutputStream()
   */
  public OutputStream getOutputStream() throws IOException
  {
    connect();
    return c.getOutputStream();
  }

  /**
   * @see HttpURLConnection#setDoOutput(boolean)
   */
  public void setDoOutput( boolean b )
  {
    c.setDoOutput(b);
  }

  /**
   * @see HttpURLConnection#setDoInput(boolean)
   */
  public void setDoInput( boolean b )
  {
    c.setDoInput(b);
  }

  /**
   * @see HttpURLConnection#getDoInput()
   */
  public boolean getDoInput()
  {
    return c.getDoInput();
  }

  /**
   * @see HttpURLConnection#getDoOutput()
   */
  public boolean getDoOutput()
  {
    return c.getDoOutput();
  }

  /**
   * @see HttpURLConnection#setUseCaches(boolean)
   */
  public void setUseCaches( boolean b )
  {
    c.setUseCaches(b);
  }

  /**
   * @see HttpURLConnection#getUseCaches()
   */
  public boolean getUseCaches()
  {
    return c.getUseCaches();
  }

  /**
   * @see HttpURLConnection#setRequestProperty(String, String)
   */
  public void setRequestProperty( String key, String value )
  {
    c.setRequestProperty(key, value);
  }

  /**
   * @see HttpURLConnection#addRequestProperty(String, String)
   */
  public void addRequestProperty( String key, String value )
  {
    c.addRequestProperty(key, value);
  }

  /**
   * @see HttpURLConnection#getRequestProperty(String)
   */
  public String getRequestProperty( String key )
  {
    return c.getRequestProperty(key);
  }

  public Map<String, List<String>> getRequestProperties()
  {
    return c.getRequestProperties();
  }

  public synchronized Reader getReader() throws IOException
  {
    if (reader == null) {
      Charset charset = getResponseCharset(true);
      if (charset == null)
        throw new IOException("No response charset");

      reader = new InputStreamReader(getInputStream(), charset);
    }
    return reader;
  }

  public String getBody() throws IOException
  {
    if (body == null) {
      synchronized (this) {
        if (body == null) {
          connect();
          try {
            try (Reader r = getReader()) {
              body = IOUtils.toString(r);
            }
          } finally {
            disconnect();
          }
        }
      }
    }
    return body;
  }

  public Callable<String> getBodyAsCallable()
  {
    return new Callable<String>()
      {
        @Override
        public String call() throws Exception
        {
          return getBody();
        }
      };
  }

  private synchronized void parseContentType() throws IOException
  {
    connect();
    try {
      responseContentType = Parsers.getContentType(c);
    } catch (IllegalArgumentException ex) {
      throw new IOException(ex);
    }
    if (responseContentType == null)
      responseContentType = Parsers.EMPTY_CONTENT_TYPE;
  }

  public String getResponseMimeType() throws IOException
  {
    if (responseContentType == null)
      parseContentType();
    return responseContentType.getMimeType();
  }

  public Charset getResponseCharset( boolean allowDefault ) throws IOException
  {
    if (responseContentType == null)
      parseContentType();
    return (responseContentType.getCharset() != null || !allowDefault) ?
      responseContentType.getCharset() :
      defaultCharset;
  }

  /**
   * @see HttpURLConnection#setRequestMethod(String)
   */
  public void setRequestMethod( String method ) throws ProtocolException
  {
    c.setRequestMethod(method);
  }

  /**
   * @see HttpURLConnection#getRequestMethod()
   */
  public String getRequestMethod()
  {
    return c.getRequestMethod();
  }

  /**
   * @see HttpURLConnection#getResponseCode()
   */
  public int getResponseCode() throws IOException
  {
    connect();
    return c.getResponseCode();
  }

  /**
   * @see HttpURLConnection#getResponseMessage()
   */
  public String getResponseMessage() throws IOException
  {
    connect();
    return c.getResponseMessage();
  }

  /**
   * @see HttpURLConnection#getHeaderFieldKey(int)
   */
  public String getHeaderFieldKey( int n )
  {
    return c.getHeaderFieldKey(n);
  }

  /**
   * @see HttpURLConnection#getHeaderField(int)
   */
  public String getHeaderField( int n )
  {
    return c.getHeaderField(n);
  }

  /**
   * @see HttpURLConnection#disconnect()
   */
  public void disconnect()
  {
    state = DISCONNECTED;
    c.disconnect();
  }

  /**
   * @see HttpURLConnection#getErrorStream()
   */
  public InputStream getErrorStream()
  {
    return c.getErrorStream();
  }

  public int getState()
  {
    return state;
  }

  /**
   * @see HttpURLConnection#setChunkedStreamingMode(int)
   */
  public void setChunkedStreamingMode( int chunklen )
  {
    c.setChunkedStreamingMode(chunklen);
  }
}
