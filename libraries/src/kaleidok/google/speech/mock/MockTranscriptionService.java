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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MockTranscriptionService extends TranscriptionService
{
  static final Logger logger =
    Logger.getLogger(MockTranscriptionService.class.getPackage().getName());


  private final HttpServer server;


  public static MockTranscriptionService newInstance( String accessKey,
    FutureCallback<SttResponse> resultHandler, STT stt )
  {
    URI apiBase;
    HttpServer server;
    try
    {
      server = HttpServer.create(
        new InetSocketAddress(InetAddress.getByName(null), 0), 0);
      server.createContext(
        DEFAULT_API_BASE.getPath(), new MockSpeechToTextHandler(stt));
      InetSocketAddress addr = server.getAddress();
      apiBase = new URI(
        "http", null, addr.getHostString(), addr.getPort(),
        DEFAULT_API_BASE.getPath(), null, null);
    }
    catch (IllegalArgumentException | URISyntaxException ex)
    {
      throw new AssertionError(ex);
    }
    catch (IOException ex)
    {
      throw new IOError(ex);
    }

    return new MockTranscriptionService(server, apiBase, accessKey,
      resultHandler);
  }


  protected MockTranscriptionService( HttpServer server, URI apiBase,
    String accessKey, FutureCallback<SttResponse> resultHandler )
  {
    super(apiBase, accessKey, resultHandler);

    logger.log(Level.CONFIG,
      "You set your Google API access key to \"{0}\"; " +
        "speech transcription is performed by {1}",
      new Object[]{ accessKey, getClass().getName() });

    server.start();
    this.server = server;
  }


  @Override
  public void shutdownNow()
  {
    super.shutdownNow();
    server.stop(0);
  }
}
