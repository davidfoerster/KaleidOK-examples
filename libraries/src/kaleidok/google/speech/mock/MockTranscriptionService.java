package kaleidok.google.speech.mock;

import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import kaleidok.google.speech.TranscriptionService;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOError;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MockTranscriptionService extends TranscriptionService
{
  static final Logger logger =
    Logger.getLogger(MockTranscriptionService.class.getPackage().getName());


  public MockTranscriptionService( String accessKey,
    FutureCallback<SttResponse> resultHandler, STT stt )
  {
    super(accessKey, resultHandler);

    HttpServer server;
    try
    {
      server = HttpServer.create(
        new InetSocketAddress(InetAddress.getByName(null), 0), 0);
      server.createContext(
        DEFAULT_API_BASE.getPath(), new MockSpeechToTextHandler(stt));
      InetSocketAddress addr = server.getAddress();
      try {
        setApiBase(new URL(
          "http", addr.getHostString(), addr.getPort(),
          DEFAULT_API_BASE.getPath()));
      } catch (MalformedURLException ex) {
        throw new AssertionError(ex);
      }
      logger.log(Level.FINER, "Running {0} at {1}",
        new Object[]{ getClass().getSimpleName(), getApiBase() });
    }
    catch (IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }
    catch (IOException ex)
    {
      throw new IOError(ex);
    }

    logger.log(Level.CONFIG,
      "You set your Google API access key to \"{0}\"; " +
        "speech transcription is performed by {1}",
      new Object[]{accessKey, this.getClass().getCanonicalName()});

    server.start();
  }
}
