package kaleidok.google.speech.mock;

import kaleidok.google.speech.STT;
import com.sun.net.httpserver.HttpExchange;
import kaleidok.http.requesthandler.MockRequestHandlerBase;
import kaleidok.http.util.Parsers;
import kaleidok.io.platform.PlatformPaths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import static kaleidok.http.util.URLEncoding.DEFAULT_CHARSET;
import static kaleidok.io.Files.NO_ATTRIBUTES;
import static kaleidok.util.AssertionUtils.fastAssert;
import static kaleidok.util.AssertionUtils.fastAssertFmt;
import static kaleidok.util.logging.LoggingUtils.logThrown;


@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public class MockSpeechToTextHandler extends MockRequestHandlerBase
{
  private static final Logger logger =
    Logger.getLogger(MockSpeechToTextHandler.class.getCanonicalName());

  final STT stt;

  private volatile MockTranscriptionService transcriptionService;


  public MockSpeechToTextHandler( STT stt )
  {
    this.stt = stt;
  }


  synchronized void setTranscriptionService(
    MockTranscriptionService transcriptionService )
  {
    if (this.transcriptionService != null)
      throw new IllegalStateException("Transcription service is already set");

    this.transcriptionService = transcriptionService;
  }


  @Override
  protected void doHandle( HttpExchange t ) throws IOException
  {
    if (transcriptionService == null)
      throw new IllegalStateException("Transcription service wasn't set");

    String contextPath = t.getHttpContext().getPath(),
      uriPath = t.getRequestURI().getPath(),
      pathWithinContext = uriPath.substring(contextPath.length());

    switch (pathWithinContext)
    {
    case "recognize":
      if (handleRecognize(t))
        break;
      // fall through

    default:
      t.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
      break;
    }
  }


  protected boolean handleRecognize( HttpExchange t ) throws IOException
  {
    if (!"POST".equals(t.getRequestMethod()))
    {
      t.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
      return true;
    }

    Map<String, String> q =
      Parsers.getQueryMap(t.getRequestURI(), DEFAULT_CHARSET);
    fastAssert(q != null && !q.isEmpty(), "No request parameters");
    fastAssert("json".equals(q.get("output")),
      "Invalid request parameter value: output=" + q.get("output"));
    fastAssert(!StringUtils.isEmpty(q.get("key")),
      "Empty request parameter: key");
    fastAssert(!StringUtils.isEmpty(q.get("lang")),
      "Empty request parameter: lang");
    fastAssert(q.size() == 3, "Superfluous request parameters");

    ContentType contentType =
      getContentType(t.getRequestHeaders(), "audio/x-flac");
    double sampleRate;
    try
    {
      sampleRate = Double.parseDouble(contentType.getParameter("rate"));
      if (sampleRate <= 0 || !Double.isFinite(sampleRate))
      {
        //noinspection ThrowCaughtLocally
        throw new IllegalArgumentException(
          "Sampling rate must be positive and finite");
      }
    }
    catch (IllegalArgumentException ex)
    {
      throw new AssertionError(
        "Invalid request content-type parameter: rate=" +
          contentType.getParameter("rate"),
        ex);
    }

    byte[] flacBuffer;
    try (InputStream is = t.getRequestBody()) {
      flacBuffer = IOUtils.toByteArray(is);
    }
    logger.log(Level.FINEST,
      "Received {0} bytes on request to {1}",
      new Object[]{flacBuffer.length, t.getRequestURI()});
    fastAssert(flacBuffer.length > 86,
      "Transmitted data only seems to contain FLAC header");

    Path tmpFilePath = createFlacLogFile();
    if (tmpFilePath != null) {
      try (OutputStream os = Files.newOutputStream(tmpFilePath)) {
        os.write(flacBuffer);
      }
    }

    double duration = testFlacFile(flacBuffer, sampleRate);
    if (!Double.isNaN(duration)) {
      fastAssert(duration <= stt.getMaxTranscriptionInterval(),
        "FLAC stream duration exceeds maximum transcription interval");
    } else {
      logger.finest("Couldn't determine duration of the submitted audio record");
    }

    byte[] transcriptionResult = normalTranscriptionResult;
    fastAssert(transcriptionResult.length != 0);
    setContentType(t, ContentType.APPLICATION_JSON);
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
    SAMPLE_RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d*)?) (k)?Hz"),
    SAMPLE_COUNT_PATTERN = Pattern.compile("(\\d+) samples");

  private static double testFlacFile( byte[] flacData, double expectedSampleRate )
  {
    Process pr;
    String fileOutput;
    try
    {
      pr = pbFlacFileTest.start();
      try (OutputStream os = pr.getOutputStream()) {
        os.write(flacData);
      }
      try (BufferedReader r =
        new BufferedReader(new InputStreamReader(pr.getInputStream())))
      {
        fileOutput = r.readLine();
        fastAssert(fileOutput == null || r.read() == -1);
      }
    }
    catch (IOException ex)
    {
      logger.log(Level.WARNING,
        "Your system configuration doesn't permit the validation of submitted audio data",
        ex);
      return Double.NaN;
    }

    while (true)
    {
      try
      {
        fastAssert(pr.waitFor() == 0);
        break;
      }
      catch (InterruptedException ex)
      {
        logThrown(logger, Level.FINEST,
          "Waiting for termination of {0} was interrupted", ex, pr);
      }
    }

    fastAssert(fileOutput != null, "The type of the sent data is unknown");
    int p = fileOutput.indexOf(':');
    fastAssert(p >= 0);
    fileOutput = fileOutput.substring(p + 2);
    String[] fileSpec = fileOutput.split(", ");
    fastAssert(fileSpec[0].startsWith("FLAC"),
      "The sent data doesn't look like a FLAC stream: " + fileOutput);

    double sampleRate = Double.NaN;
    long sampleCount = -1;
    for (int i = 1; i < fileSpec.length; i++)
    {
      String s = fileSpec[i];
      Matcher m;
      if ((m = SAMPLE_RATE_PATTERN.matcher(s)).matches())
      {
        try {
          sampleRate = Double.parseDouble(m.group(1));
        } catch (NumberFormatException ex) {
          throw new AssertionError(s, ex);
        }

        if (m.groupCount() >= 2)
        {
          switch (s.charAt(m.start(2)))
          {
          case 'k':
            sampleRate *= 1000;
            break;

          default:
            throw new AssertionError(
              "Unsupported magnitude prefix: " + m.group(2));
          }
        }
      }
      else if ((m = SAMPLE_COUNT_PATTERN.matcher(s)).matches())
      {
        try {
          sampleCount = Long.parseLong(m.group(1));
        } catch (NumberFormatException ex) {
          throw new AssertionError(s, ex);
        }
      }
    }

    fastAssert(sampleRate > 0 && Double.isFinite(sampleRate),
      "Couldn't determine sample rate of submitted audio stream");
    fastAssertFmt(sampleRate == expectedSampleRate,
      "Sample rate of submitted audio stream (%f) doesn't match expectation (%f)",
      sampleRate, expectedSampleRate);

    return (sampleCount >= 0) ? sampleCount / sampleRate : Double.NaN;
  }


  private static final AtomicReference<Path> tempDir = new AtomicReference<>();

  @SuppressWarnings("SpellCheckingInspection")
  private static final DateFormat logfileNameFormat =
    new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS-", Locale.ROOT);


  protected Path createFlacLogFile() throws IOException
  {
    if (!transcriptionService.isLogAudioData())
      return null;

    String fn;
    synchronized (logfileNameFormat)
    {
      fn = logfileNameFormat.format(new Date());
    }

    return Files.createTempFile(getTempDir(), fn, ".flac", NO_ATTRIBUTES);
  }


  protected static Path getTempDir() throws IOException
  {
    Path tempDir = MockSpeechToTextHandler.tempDir.get();

    if (tempDir == null)
    {
      tempDir =
        PlatformPaths.getTempDir()
          .resolve(MockTranscriptionService.class.getName());
      try
      {
        Files.createDirectory(tempDir, NO_ATTRIBUTES);
      }
      finally
      {
        if (!MockSpeechToTextHandler.tempDir.compareAndSet(null, tempDir))
        {
          Path tempDir2 = MockSpeechToTextHandler.tempDir.get();
          if (!tempDir.equals(tempDir2))
          {
            logger.log(Level.WARNING,
              "Temporary directory path was updated concurrently with " +
                "different values: \"{0}\" ≠ \"{1}\"",
              new Object[]{ tempDir, tempDir2 });
          }
          tempDir = tempDir2;
        }
      }
    }

    return tempDir;
  }


  @SuppressWarnings({ "unused", "HardcodedLineSeparator" })
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
      ).getBytes(ContentType.APPLICATION_JSON.getCharset()),

    emptyTranscriptionResult =
      "{\"result\":[]}"
        .getBytes(ContentType.APPLICATION_JSON.getCharset());
}
