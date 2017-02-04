package kaleidok.net.http;

import kaleidok.net.http.util.MimeTypeMap;
import kaleidok.net.http.util.Parsers;
import org.apache.commons.io.IOUtils;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static kaleidok.net.http.HttpConnection.ConnectionState.CONNECTED;
import static kaleidok.net.http.HttpConnection.ConnectionState.DISCONNECTED;
import static kaleidok.net.http.HttpConnection.ConnectionState.UNCONNECTED;


public class HttpConnection implements Closeable
{
  public static final String HTTP_PROTOCOL = "http";

  final HttpURLConnection c;

  private volatile MimeTypeMap acceptedMimeTypes;

  public Charset defaultCharset;

  private volatile ContentType responseContentType;

  private InputStream inputStream;

  private Reader reader;

  private volatile String body;


  public enum ConnectionState
  {
    UNCONNECTED,
    CONNECTED,
    DISCONNECTED
  }

  private volatile ConnectionState state = UNCONNECTED;


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
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }


  @SuppressWarnings("ProhibitedExceptionDeclared")
  protected static <T extends HttpConnection>
  T openURL( URL url, Class<T> clazz )
    throws IOException, InstantiationException, NoSuchMethodException,
    InvocationTargetException, ClassCastException
  {
    if (!HttpConnection.class.isAssignableFrom(clazz))
    {
      throw new ClassCastException(String.format(
        "Cannot cast %s to %s",
        clazz.getName(), HttpConnection.class.getName()));
    }
    if (Modifier.isAbstract(clazz.getModifiers()))
      throw new InstantiationException(clazz.getName() + " is abstract");

    checkHttpProtocol(url);
    Constructor<T> ctor = clazz.getConstructor(constructorArgumentTypes);
    URLConnection conn = url.openConnection();
    try
    {
      return ctor.newInstance(conn);
    }
    catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }
    catch (InvocationTargetException ex)
    {
      Throwable cause = ex.getCause();
      if (cause instanceof IOException)
        throw (IOException) cause;
      throw ex;
    }
  }


  public static boolean isSupported( URL url )
  {
    String p = url.getProtocol(), h = HTTP_PROTOCOL;
    int pl = p.length(), hl = h.length();
    return p.startsWith(h) && (pl == hl || (pl == hl + 1 && p.charAt(hl) == 's'));
  }


  private static void checkHttpProtocol( URL url ) throws IOException
  {
    if (!isSupported(url))
      throw new IOException("Unsupported protocol: " + url.getProtocol());
  }


  private static final Class<?>[] constructorArgumentTypes =
    new Class<?>[]{ HttpURLConnection.class };


  public MimeTypeMap getAcceptedMimeTypes()
  {
    return acceptedMimeTypes;
  }


  public synchronized void setAcceptedMimeTypes( MimeTypeMap acceptedMimeTypes )
  {
    if (state != UNCONNECTED)
      throw new IllegalStateException();

    this.acceptedMimeTypes = acceptedMimeTypes;
  }


  /**
   * @see HttpURLConnection#connect()
   */
  public void connect() throws IOException
  {
    if (!checkConnectionState()) {
      synchronized (this) {
        if (!checkConnectionState()) {
          MimeTypeMap acceptedMimeTypes = this.acceptedMimeTypes;
          if (acceptedMimeTypes != null) {
            acceptedMimeTypes.freeze();
            String accept = acceptedMimeTypes.toString();
            if (!accept.isEmpty())
              c.setRequestProperty("Accept", accept);
          }

          c.connect();
          state = CONNECTED;
        }
      }
    }
  }


  private boolean checkConnectionState()
  {
    switch (state)
    {
    case UNCONNECTED:
      return false;

    case CONNECTED:
      return true;

    default:
      throw new IllegalStateException();
    }
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


  public String getContentTypeString()
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
        MimeTypeMap acceptedMimeTypes = this.acceptedMimeTypes;
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
        inputStream =
          Parsers.DECODERS.getDecodedStream(contentEncoding, rawInputStream);

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


  public synchronized String getBody() throws IOException
  {
    if (body == null)
    {
      try (Reader r = getReader())
      {
        body = IOUtils.toString(r);
      }
      finally
      {
        disconnect();
      }
    }
    return body;
  }


  public Callable<String> getBodyAsCallable()
  {
    return this::getBody;
  }


  public ContentType getContentType() throws IOException
  {
    if (responseContentType == null)
    {
      synchronized (this)
      {
        if (responseContentType == null)
        {
          connect();

          try
          {
            responseContentType = Parsers.getContentType(c);
          }
          catch (ParseException | IllegalArgumentException ex)
          {
            throw new IOException(ex);
          }
        }
      }
    }
    return responseContentType;
  }


  public String getResponseMimeType() throws IOException
  {
    return getContentType().getMimeType();
  }


  public Charset getResponseCharset( boolean allowDefault ) throws IOException
  {
    Charset chs = getContentType().getCharset();
    return (chs != null || !allowDefault) ? chs : defaultCharset;
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


  @Override
  public void close()
  {
    disconnect();
  }


  /**
   * @see HttpURLConnection#getErrorStream()
   */
  public InputStream getErrorStream()
  {
    return c.getErrorStream();
  }


  public ConnectionState getState()
  {
    return state;
  }


  /**
   * @see HttpURLConnection#setChunkedStreamingMode(int)
   */
  public void setChunkedStreamingMode( int chunkLen )
  {
    c.setChunkedStreamingMode(chunkLen);
  }
}
