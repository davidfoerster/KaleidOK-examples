package kaleidok.http.requesthandler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kaleidok.http.util.Parsers;
import org.apache.commons.io.IOUtils;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Map;

import static kaleidok.http.util.URLEncoding.DEFAULT_CHARSET;
import static kaleidok.util.AssertionUtils.fastAssert;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.entity.ContentType.WILDCARD;


public abstract class MockRequestHandlerBase implements HttpHandler
{
  public static final String
    CONTENT_TYPE = "Content-Type",
    CONTENT_LENGTH = "Content-Length";


  @SuppressWarnings({ "OverlyBroadCatchBlock", "ProhibitedExceptionCaught", "ErrorNotRethrown" })
  @Override
  public void handle( HttpExchange exchange ) throws IOException
  {
    try
    {
      doHandle(exchange);
    }
    catch (NullPointerException | NumberFormatException | ParseException |
      UnsupportedCharsetException | AssertionError ex)
    {
      ex.printStackTrace();
      handleException(exchange, ex, HttpURLConnection.HTTP_BAD_REQUEST);
    }
    catch (Throwable ex)
    {
      ex.printStackTrace();
      handleException(exchange, ex, HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
    finally
    {
      exchange.close();
    }
  }


  protected abstract void doHandle( HttpExchange exchange ) throws IOException;


  protected static void setContentType( HttpExchange t, ContentType contentType )
  {
    t.getResponseHeaders().set(CONTENT_TYPE, contentType.toString());
  }


  protected static void handleException( HttpExchange t, Throwable ex,
    int responseCode )
    throws IOException
  {
    if (t.getResponseCode() >= 0)
      return;

    Charset charset = DEFAULT_CHARSET;
    ByteArrayOutputStream buf = new ByteArrayOutputStream(1 << 9);
    try (PrintWriter out =
      new PrintWriter(new OutputStreamWriter(buf, charset), false))
    {
      ex.printStackTrace(out);
    }

    if (buf.size() > 0)
    {
      setContentType(t, ContentType.TEXT_PLAIN.withCharset(charset));
      t.sendResponseHeaders(responseCode, buf.size());
      try (OutputStream out = t.getResponseBody())
      {
        buf.writeTo(out);
      }
    }
    else
    {
      t.sendResponseHeaders(responseCode, -1);
    }
  }


  protected static Map<String, String> getFormData( HttpExchange t )
    throws IOException
  {
    Headers headers = t.getRequestHeaders();
    ContentType contentType =
      getContentType(headers, APPLICATION_FORM_URLENCODED.getMimeType());

    int contentLength = getContentLengthInt(headers);
    if (contentLength == 0)
      return Collections.emptyMap();

    String sFormData;
    if (contentLength > 0)
    {
      final byte[] buf = new byte[contentLength];
      int count;
      try (InputStream bodyStream = t.getRequestBody()) {
        count = IOUtils.read(bodyStream, buf);
      }
      fastAssert(count == buf.length, "Content length mismatch");

      Charset charset = contentType.getCharset();
      sFormData =
        new String(buf, (charset != null) ? charset : DEFAULT_CHARSET);
    }
    else
    {
      try (InputStream bodyStream = t.getRequestBody()) {
        sFormData = IOUtils.toString(bodyStream, contentType.getCharset());
      }
    }
    return Parsers.getQueryMap(sFormData, DEFAULT_CHARSET);
  }


  protected static ContentType getContentType( Headers h,
    String expectedMimeType )
    throws ParseException, UnsupportedCharsetException
  {
    String sContentType = h.getFirst(CONTENT_TYPE);
    ContentType contentType =
      (sContentType != null) ? ContentType.parse(sContentType) : WILDCARD;
    if (expectedMimeType.equals(contentType.getMimeType()))
      return contentType;

    throw new AssertionError("Expected " + expectedMimeType + " data");
  }


  protected static long getContentLength( Headers h )
  {
    return getContentLength(h, Long.MAX_VALUE);
  }

  protected static int getContentLengthInt( Headers h )
  {
    return (int) getContentLength(h, Integer.MAX_VALUE);
  }

  protected static long getContentLength( Headers h, long maxValue )
    throws NumberFormatException
  {
    String sContentLength = h.getFirst(CONTENT_LENGTH);
    if (sContentLength == null)
      return -1;

    long contentLength;
    try
    {
      contentLength = Long.parseUnsignedLong(sContentLength);
    }
    catch (NumberFormatException ex)
    {
      throw new NumberFormatException(
        "Illegal " + CONTENT_LENGTH + " value: " + ex.getMessage());
    }

    if (contentLength >= 0 && contentLength <= maxValue)
      return contentLength;

    throw new NumberFormatException(
      CONTENT_LENGTH + " value too large: " + sContentLength);
  }


  protected static boolean isEmpty( HttpExchange exchange ) throws IOException
  {
    if (getContentLength(exchange.getRequestHeaders()) < 0)
      return true;

    try (InputStream in = exchange.getRequestBody()) {
      return in.read() == -1;
    }
  }
}
