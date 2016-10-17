package kaleidok.google.speech.mock;

import com.sun.net.httpserver.HttpContext;
import javafx.beans.property.ReadOnlyObjectProperty;
import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import com.sun.net.httpserver.HttpServer;
import kaleidok.google.speech.TranscriptionServiceBase;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOError;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static kaleidok.google.speech.TranscriptionService.DEFAULT_API_BASE;


public class MockTranscriptionService extends TranscriptionServiceBase
{
  static final Logger logger =
    Logger.getLogger(MockTranscriptionService.class.getPackage().getName());


  private final HttpContext context;


  public static MockTranscriptionService newInstance( String accessKey,
    FutureCallback<SttResponse> resultHandler, STT stt )
  {
    HttpContext context;
    try
    {
      HttpServer server = HttpServer.create(
        new InetSocketAddress(InetAddress.getByName(null), 0), 0);
      context = server.createContext(
        DEFAULT_API_BASE.getPath(), new MockSpeechToTextHandler(stt));
    }
    catch (IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }
    catch (IOException ex)
    {
      throw new IOError(ex);
    }

    return new MockTranscriptionService(context, accessKey, resultHandler);
  }


  protected MockTranscriptionService( HttpContext context, String accessKey,
    FutureCallback<SttResponse> resultHandler )
  {
    super(uriFromContext(context), accessKey, resultHandler);

    logger.log(Level.CONFIG,
      "You set your Google API access key to \"{0}\"; " +
        "speech transcription is performed by {1}",
      new Object[]{ accessKey, getClass().getName() });

    context.getServer().start();
    this.context = context;
  }


  private static URI uriFromContext( HttpContext context )
  {
    InetSocketAddress addr = context.getServer().getAddress();
    try {
      return new URI(
        "http", null, addr.getHostString(), addr.getPort(),
        context.getPath(), null, null);
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


  @Override
  public ReadOnlyObjectProperty<URI> apiBaseProperty()
  {
    return apiBase.getReadOnlyProperty();
  }


  @Override
  public void setApiBase( URI apiBase )
  {
    if (!Objects.equals(apiBase, this.apiBase.get()))
    {
      logger.log(Level.CONFIG,
        "Trying to set property \"{0}\" to \"{1}\" but it's part of a mock " +
          "implementation and therefore read-only.",
        new Object[]{ this.apiBase.getName(), apiBase });
    }
  }


  @Override
  public void shutdownNow()
  {
    super.shutdownNow();
    context.getServer().stop(0);
  }
}
