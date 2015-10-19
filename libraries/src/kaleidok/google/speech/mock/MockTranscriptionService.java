package kaleidok.google.speech.mock;

import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import kaleidok.google.speech.TranscriptionService;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.concurrent.FutureCallback;

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

  private static URL MOCK_API_BASE = null;

  private static HttpServer server = null;

  private static MockSpeechToTextHandler sttHandler;


  protected static void initServer( STT stt, InetSocketAddress addr ) throws IOException
  {
    boolean createNew = false;
    if (server == null) {
      synchronized (MockTranscriptionService.class) {
        if (server == null) {
          server = HttpServer.create(addr, 0);
          sttHandler = new MockSpeechToTextHandler(stt);
          server.createContext(DEFAULT_API_BASE.getPath(), sttHandler);
          server.start();
          addr = server.getAddress();
          try {
            MOCK_API_BASE = new URL(
              "http", addr.getHostString(), addr.getPort(),
              DEFAULT_API_BASE.getPath());
          } catch (MalformedURLException ex) {
            throw new AssertionError(ex);
          }
          createNew = true;
        }
      }
    }
    if (createNew) {
      logger.log(Level.FINER, "Running {0} at {1}",
        new Object[]{MockTranscriptionService.class.getSimpleName(), MOCK_API_BASE});
    } else {
      assert stt == sttHandler.stt && server.getAddress().equals(addr);
    }
  }


  private static URL initServerThrowError( STT stt, String hostName, int port )
  {
    try {
      InetAddress hostAddr = InetAddress.getByName(hostName);
      initServer(stt, new InetSocketAddress(hostAddr, port));
      return MOCK_API_BASE;
    } catch (IOException ex) {
      throw new Error(ex);
    }
  }


  public MockTranscriptionService( String accessKey,
    FutureCallback<SttResponse> resultHandler, STT stt )
  {
    super(initServerThrowError(stt, null, 0), accessKey, resultHandler);

    String className = this.getClass().getCanonicalName();
    logger.log(Level.CONFIG,
      "You set your Google API access key to \"{0}\"; " +
        "speech transcription is performed by {1}",
      new Object[]{accessKey, className});
  }
}
