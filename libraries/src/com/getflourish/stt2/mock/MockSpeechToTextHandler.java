package com.getflourish.stt2.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kaleidok.http.util.Parsers;
import kaleidok.io.platform.PlatformPaths;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static kaleidok.http.URLEncoding.DEFAULT_CHARSET;


public class MockSpeechToTextHandler implements HttpHandler
{
  @Override
  public void handle( HttpExchange t ) throws IOException
  {
    String contextPath = t.getHttpContext().getPath(),
      uriPath = t.getRequestURI().getPath(),
      pathWithinContext = uriPath.substring(contextPath.length());

    try {
      switch (pathWithinContext) {
      case "recognize":
        if (handleRecognize(t))
          break;

      default:
        t.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
        break;
      }
    } catch (NullPointerException | IllegalArgumentException ex) {
      ex.printStackTrace();
      handleException(t, ex, HttpURLConnection.HTTP_BAD_REQUEST);
    } catch (Throwable ex) {
      ex.printStackTrace();
      handleException(t, ex, HttpURLConnection.HTTP_INTERNAL_ERROR);
    } finally {
      t.close();
    }
  }


  protected boolean handleRecognize( HttpExchange t ) throws IOException
  {
    if (!"POST".equals(t.getRequestMethod())) {
      t.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
      return true;
    }

    Map<String, String> q =
      Parsers.getQueryMap(t.getRequestURI(), DEFAULT_CHARSET);
    assertTrue(q != null);
    assertTrue("json".equals(q.get("output")));
    assertTrue(!q.get("key").isEmpty());
    assertTrue(!q.get("lang").isEmpty());
    assertTrue(q.size() == 3);

    Map<String, String> contentType =
      Parsers.getHeaderValueMap(t.getRequestHeaders().getFirst(CONTENT_TYPE));
    assertTrue("audio/x-flac".equals(contentType.get(null)));
    float sampleRate = Float.parseFloat(contentType.get("rate"));
    assertTrue(sampleRate > 0 && !Float.isInfinite(sampleRate));

    long inLen;
    try (InputStream in = t.getRequestBody()) {
      inLen =
        Files.copy(in, createTempFile(this, ".flac"),
          StandardCopyOption.REPLACE_EXISTING);
    }
    System.out.println("Server received " + inLen + " bytes on request to " + t.getRequestURI());
    assertTrue(inLen > 86);

    setContentType(t, "application/json");
    t.sendResponseHeaders(HttpURLConnection.HTTP_OK, transcriptionResult.length);
    try (OutputStream out = t.getResponseBody()) {
      out.write(transcriptionResult);
    }

    return true;
  }


  private static Path tempDir = null;

  private static final Format tempFileFormat =
    new SimpleDateFormat("yyyyMMdd-HHmmss.SSS-");

  protected static Path createTempFile( Object namespace, String extension )
    throws IOException
  {
    Class<?> clazz = (namespace instanceof Class) ? (Class<?>) namespace : namespace.getClass();
    if (tempDir == null) {
      tempDir = PlatformPaths.INSTANCE.getTempDir().resolve(clazz.getCanonicalName());
      try {
        Files.createDirectory(tempDir);
      } catch (FileAlreadyExistsException ex) {
        // go on...
      }
    }
    return Files.createTempFile(tempDir, tempFileFormat.format(new Date()),
      extension);
  }


  protected static void assertTrue( boolean b )
  {
    if (!b)
      throw new IllegalArgumentException();
  }


  protected static void setContentType( HttpExchange t, String contentType )
  {
    t.getResponseHeaders().set(CONTENT_TYPE,
      contentType + "; charset=" + DEFAULT_CHARSET.name() + ';');
  }


  protected static void handleException( HttpExchange t, Throwable ex, int responseCode )
    throws IOException
  {
    if (t.getResponseCode() < 0) {
      setContentType(t, "text/plain");
      t.sendResponseHeaders(responseCode, 0);
      try (PrintStream out = new PrintStream(t.getResponseBody(), false, DEFAULT_CHARSET.name())) {
        ex.printStackTrace(out);
      }
    }
  }


  public static final String CONTENT_TYPE = "Content-Type";


  private static final byte[] transcriptionResult = (
    "{\"result\":[]}\n" +
      "{\"result\":[{" +
      "\"alternative\":[" +
      "{\"transcript\":\"I like hot dogs\",\"confidence\":0.95803052}," +
      "{\"transcript\":\"I like hotdogs\"}," +
      "{\"transcript\":\"I like hot stocks\"}," +
      "{\"transcript\":\"I'll like hotdogs\"}," +
      "{\"transcript\":\"I like a hot stocks\"}" +
      "]," +
      "\"final\":true" +
      "}]," +
      "\"result_index\":0}\n"
  ).getBytes(DEFAULT_CHARSET);
}
