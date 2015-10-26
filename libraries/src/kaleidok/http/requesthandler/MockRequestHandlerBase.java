package kaleidok.http.requesthandler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kaleidok.http.util.Parsers;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.Map;

import static kaleidok.http.URLEncoding.DEFAULT_CHARSET;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;


public abstract class MockRequestHandlerBase implements HttpHandler
{
  public static final String
    CONTENT_TYPE = "Content-Type",
    CONTENT_LENGTH = "content-length";


  @Override
  public void handle( HttpExchange exchange ) throws IOException
  {
    try {
      doHandle(exchange);
    } catch (NullPointerException | NumberFormatException | AssertionError ex) {
      ex.printStackTrace();
      handleException(exchange, ex, HttpURLConnection.HTTP_BAD_REQUEST);
    } catch (Throwable ex) {
      ex.printStackTrace();
      handleException(exchange, ex, HttpURLConnection.HTTP_INTERNAL_ERROR);
    } finally {
      exchange.close();
    }
  }


  protected abstract void doHandle( HttpExchange exchange ) throws IOException;


  protected static void setContentType( HttpExchange t, ContentType contentType )
  {
    t.getResponseHeaders().set(CONTENT_TYPE, contentType.toString());
  }


  protected static void handleException( HttpExchange t, Throwable ex, int responseCode )
    throws IOException
  {
    if (t.getResponseCode() < 0) {
      setContentType(t, ContentType.TEXT_PLAIN.withCharset(DEFAULT_CHARSET));
      t.sendResponseHeaders(responseCode, 0);
      try (PrintStream out = new PrintStream(t.getResponseBody(), false, DEFAULT_CHARSET.name())) {
        ex.printStackTrace(out);
      }
    }
  }


  protected static Map<String, String> getFormData( HttpExchange t ) throws IOException
  {
    Headers headers = t.getRequestHeaders();
    ContentType contentType = ContentType.parse(headers.getFirst(CONTENT_TYPE));
    assert APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType.getMimeType()) :
      "Expected " + APPLICATION_FORM_URLENCODED.getMimeType() + " data";
    String sContentLength = headers.getFirst(CONTENT_LENGTH);
    String sFormData;
    if (sContentLength != null) {
      final byte[] buf = new byte[Integer.parseInt(sContentLength)];
      long count = 0;
      try (InputStream bodyStream = t.getRequestBody()) {
        int rv;
        while (count < buf.length && (rv = bodyStream.read(buf, (int) count, buf.length)) >= 0)
          count += rv;
      }
      assert count == buf.length : "Content length mismatch";
      sFormData = new String(buf, contentType.getCharset());
    } else {
      try (InputStream bodyStream = t.getRequestBody()) {
        sFormData = IOUtils.toString(bodyStream, contentType.getCharset());
      }
    }
    return Parsers.getQueryMap(sFormData, DEFAULT_CHARSET);
  }


  protected static boolean isEmpty( HttpExchange exchange ) throws IOException
  {
    String contentLength = exchange.getRequestHeaders().getFirst(CONTENT_LENGTH);
    if (contentLength != null && Long.parseLong(contentLength) != 0)
      return false;
    try (InputStream in = exchange.getRequestBody()) {
      return in.read() == -1;
    }
  }
}