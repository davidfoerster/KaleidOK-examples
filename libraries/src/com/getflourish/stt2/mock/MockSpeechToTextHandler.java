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
import static kaleidok.util.Strings.isEmpty;


public class MockSpeechToTextHandler implements HttpHandler
{
  static {
    Class<?> clazz = MockSpeechToTextHandler.class;
    clazz.getClassLoader()
      .setPackageAssertionStatus(clazz.getPackage().getName(), true);
  }


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
    if (q == null || q.isEmpty())
      throwInvalid("No request parameters");
    if (!"json".equals(q.get("output")))
      throwInvalid("Invalid request parameter value: output=" + q.get("output"));
    if (isEmpty(q.get("key")))
      throwInvalid("Empty request parameter: key");
    if (isEmpty(q.get("lang")))
      throwInvalid("Empty request parameter: lang");
    if (q.size() != 3)
      throwInvalid("Superfluous request parameters");

    Map<String, String> contentType =
      Parsers.getHeaderValueMap(t.getRequestHeaders().getFirst(CONTENT_TYPE));
    if (!"audio/x-flac".equals(contentType.get(null)))
      throwInvalid("Invalid request content-type: " + contentType.get(null));
    float sampleRate;
    try {
      sampleRate = Float.parseFloat(contentType.get("rate"));
      if (!(sampleRate > 0 && !Float.isInfinite(sampleRate)))
        throw new IllegalArgumentException("Sampling rate must be positive and finite");
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
        "Invalid request content-type parameter: rate=" + contentType.get("rate"), ex);
    }

    byte[] flacBuffer;
    try (InputStream is = t.getRequestBody()) {
      flacBuffer = IOUtils.toByteArray(is);
    }
    logger.log(Level.FINEST,
      "Received {0} bytes on request to {1}",
      new Object[]{flacBuffer.length, t.getRequestURI()});
    if (flacBuffer.length <= 86)
      throwInvalid("Transmitted data only seems to contain FLAC header");

    Path tmpFilePath = createTempFile();
    if (tmpFilePath != null) {
      try (OutputStream os = Files.newOutputStream(tmpFilePath)) {
        os.write(flacBuffer);
      }
    }

    double duration = testFlacFile(flacBuffer, sampleRate);
    if (!Double.isNaN(duration)) {
      if (duration > stt.getMaxTranscriptionInterval() * 1e-9)
        throwInvalid("FLAC stream duration exceeds maximum transcription interval");
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
          assert r.read() == -1;
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING,
        "Your system configuration doesn't permit the validation of submitted audio data",
        ex);
      return Double.NaN;
    }
    while (true) {
      try {
        assert pr.waitFor() == 0;
        break;
      } catch (InterruptedException ex) {
        // keep waiting
      }
    }

    if (fileOutput == null)
      throwInvalid("The type of the sent data is unknown");
    int p = fileOutput.indexOf(':');
    assert p >= 0;
    String[] fileSpec = fileOutput.substring(p + 2).split(", ");
    if (!fileSpec[0].startsWith("FLAC"))
      throwInvalid("The sent data doesn't look like a FLAC stream: " + fileOutput.substring(p + 2));

    double sampleRate = Double.NaN;
    long sampleCount = -1;
    for (int i = 1; i < fileSpec.length; i++) {
      String s = fileSpec[i];
      Matcher m;
      if ((m = SAMPLERATE_PATTERN.matcher(s)).matches())
      {
        try {
          sampleRate = Double.parseDouble(m.group(1));
        } catch (NumberFormatException ex) {
          throw new AssertionError(s, ex);
        }
        assert sampleRate > 0 && !Double.isInfinite(sampleRate);

        if (m.groupCount() >= 2)
        switch (m.group(2).charAt(0)) {
        case 'k':
          sampleRate *= 1000;
          break;

        default:
          throw new AssertionError("Unsupported magnitude prefix: " + m.group(2));
        }
      }
      else if ((m = SAMPLECOUNT_PATTERN.matcher(s)).matches())
      {
        try {
          sampleCount = Long.parseLong(m.group(1));
        } catch (NumberFormatException ex) {
          throw new AssertionError(s, ex);
        }
      }
    }

    if (Double.isNaN(sampleRate))
      throwInvalid("Couldn't determine sample rate of submitted audio stream");
    if (sampleRate != expectedSampleRate) {
      throwInvalid(String.format(
        "Sample rate of submitted audio stream (%f) doesn't match expectation (%f)",
        sampleRate, expectedSampleRate));
    }

    return (sampleCount >= 0) ? sampleCount / sampleRate : Double.NaN;
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


  protected static void setContentType( HttpExchange t, String contentType )
  {
    t.getResponseHeaders().set(CONTENT_TYPE,
      contentType + "; charset=" + DEFAULT_CHARSET.name() + ';');
  }


  protected static void throwInvalid( String msg )
  {
    throw new IllegalArgumentException(msg);
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
