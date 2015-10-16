package com.getflourish.stt2.mock;

import com.getflourish.stt2.STT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kaleidok.http.util.Parsers;
import kaleidok.io.platform.PlatformPaths;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import static kaleidok.http.URLEncoding.DEFAULT_CHARSET;


public class MockSpeechToTextHandler implements HttpHandler
{
  private static final Logger logger =
    Logger.getLogger(MockSpeechToTextHandler.class.getCanonicalName());

  final STT stt;


  public MockSpeechToTextHandler( STT stt )
  {
    this.stt = stt;
  }


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
    } catch (NullPointerException | AssertionError ex) {
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

    byte[] flacBuffer;
    try (InputStream is = t.getRequestBody()) {
      flacBuffer = IOUtils.toByteArray(is);
    }
    logger.log(Level.FINEST,
      "Received {0} bytes on request to {1}",
      new Object[]{flacBuffer.length, t.getRequestURI()});
    assertTrue(flacBuffer.length > 86);

    Path tmpFilePath = createTempFile();
    if (tmpFilePath != null) {
      try (OutputStream os = Files.newOutputStream(tmpFilePath)) {
        os.write(flacBuffer);
      }
    }

    double duration = testFlacFile(flacBuffer, sampleRate);
    if (!Double.isNaN(duration)) {
      assertTrue(duration <= stt.getMaxTranscriptionInterval() * 1e-9);
    } else {
      logger.finest("Couldn't determine duration of the submitted audio record");
    }

    byte[] transcriptionResult = normalTranscriptionResult;
    setContentType(t, "application/json");
    t.sendResponseHeaders(HttpURLConnection.HTTP_OK, transcriptionResult.length);
    try (OutputStream out = t.getResponseBody()) {
      out.write(transcriptionResult);
    }

    return true;
  }


  private static final ProcessBuilder pbFlacFileTest =
    new ProcessBuilder("file", "-")
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectInput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.INHERIT);
  static {
    pbFlacFileTest.environment().put("LC_MESSAGES", "C");
  }

  private static final Pattern
    SAMPLERATE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d*)?) (k)?Hz$"),
    SAMPLECOUNT_PATTERN = Pattern.compile("^(\\d+) samples$");

  private static double testFlacFile( byte flacData[], float expectedSampleRate )
    throws IOException
  {
    Process pr;
    String fileOutput;
    try {
      pr = pbFlacFileTest.start();
      try (OutputStream os = pr.getOutputStream()) {
        os.write(flacData);
      }
      try (BufferedReader r =
        new BufferedReader(new InputStreamReader(pr.getInputStream())))
      {
        fileOutput = r.readLine();
        if (fileOutput != null)
          assertTrue(r.read() == -1);
      }
    } catch (IOException ex) {
      throw new Error(
        "Your system configuration doesn't permit the validation of submitted audio data", ex);
    }
    while (true) {
      try {
        assertTrue(pr.waitFor() == 0);
        break;
      } catch (InterruptedException ex) {
        // keep waiting
      }
    }

    assertTrue(fileOutput != null);
    int p = fileOutput.indexOf(':');
    assertTrue(p >= 0);
    String[] fileSpec = fileOutput.substring(p + 2).split(", ");
    assertTrue(fileSpec[0].startsWith("FLAC"));

    float sampleRate = Float.NaN;
    long sampleCount = -1;
    for (int i = 1; i < fileSpec.length; i++) {
      String s = fileSpec[i];
      Matcher m;
      if ((m = SAMPLERATE_PATTERN.matcher(s)).matches())
      {
        try {
          sampleRate = Float.parseFloat(m.group(1));
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException(s, ex);
        }
        if (m.groupCount() >= 2)
        switch (m.group(2).charAt(0)) {
        case 'k':
          sampleRate *= 1000;
          break;
        }
        assertTrue(sampleRate == expectedSampleRate);
      }
      else if ((m = SAMPLECOUNT_PATTERN.matcher(s)).matches())
      {
        try {
          sampleCount = Long.parseLong(m.group(1));
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException(s, ex);
        }
      }
    }

    return (sampleCount >= 0) ? (double) sampleCount / sampleRate : Double.NaN;
  }


  private static Path tempDir = null;

  protected Path createTempFile()
    throws IOException
  {
    Format logfilePattern = stt.logfilePattern;
    if (logfilePattern == null)
      return null;

    if (tempDir == null) {
      tempDir = PlatformPaths.getTempDir().resolve(this.getClass().getCanonicalName());
      try {
        Files.createDirectory(tempDir);
      } catch (FileAlreadyExistsException ex) {
        if (!Files.isDirectory(tempDir))
          throw ex;
      }
    }

    String fn = logfilePattern.format(new Date());
    int p = FilenameUtils.indexOfExtension(fn);
    return Files.createTempFile(tempDir,
      (p >= 0) ? fn.substring(0, p) : fn,
      (p >= 0) ? fn.substring(p) : ".flac",
      PlatformPaths.NO_ATTRIBUTES);
  }


  protected static void assertTrue( boolean b )
  {
    if (!b)
      throw new AssertionError();
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


  private static final byte[]
    normalTranscriptionResult = (
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
      ).getBytes(DEFAULT_CHARSET),
    emptyTranscriptionResult =
      "{\"result\":[]}".getBytes(DEFAULT_CHARSET);
}
