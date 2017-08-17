package kaleidok.google.speech.mock;

import com.sun.net.httpserver.HttpContext;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import com.sun.net.httpserver.HttpServer;
import kaleidok.google.speech.TranscriptionService;
import kaleidok.google.speech.TranscriptionServiceBase;
import kaleidok.javafx.beans.property.AspectedBooleanProperty;
import kaleidok.javafx.beans.property.AspectedObjectProperty;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringConversionPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.util.converter.UriStringConverter;
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
import java.util.stream.Stream;

import static kaleidok.google.speech.TranscriptionService.DEFAULT_API_BASE;
import static kaleidok.net.http.HttpConnection.HTTP_PROTOCOL;


public class MockTranscriptionService extends TranscriptionServiceBase
{
  private static final Logger logger =
    Logger.getLogger(MockTranscriptionService.class.getPackage().getName());


  private final HttpContext context;

  private final AspectedBooleanProperty logAudioData;


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

    logAudioData = new AspectedBooleanProperty(this, "log audio data", false);
    logAudioData.addAspect(PropertyPreferencesAdapterTag.getInstance());

    this.apiBase.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new ApiBaseDebugPropertyPreferencesAdapter(this.apiBase));

    logger.log(Level.CONFIG,
      "You set your Google API access key to \"{0}\"; " +
        "speech transcription is performed by {1}",
      new Object[]{ accessKey, getClass().getName() });

    this.context = context;
    ((MockSpeechToTextHandler) this.context.getHandler())
      .setTranscriptionService(this);
    this.context.getServer().start();
  }


  private static URI uriFromContext( HttpContext context )
  {
    InetSocketAddress addr = context.getServer().getAddress();
    try {
      return new URI(
        HTTP_PROTOCOL, null, addr.getHostString(), addr.getPort(),
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
        "Trying to set property \"{0}\" to \"{1}\" but it’s part of a mock " +
          "implementation and therefore read-only.",
        new Object[]{ this.apiBase.getName(), apiBase });
    }
  }


  public BooleanProperty logAudioDataProperty()
  {
    return logAudioData;
  }

  public boolean isLogAudioData()
  {
    return logAudioData.get();
  }

  public void setLogAudioData( boolean logAudioData )
  {
    this.logAudioData.set(logAudioData);
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.concat(
      super.getPreferenceAdapters(),
      Stream.of(logAudioData.getAspect(
        PropertyPreferencesAdapterTag.getWritableInstance())));
  }


  @Override
  public void shutdownNow()
  {
    super.shutdownNow();
    context.getServer().stop(0);
  }


  @SuppressWarnings("UnnecessarySuperQualifier")
  private static final class ApiBaseDebugPropertyPreferencesAdapter
    extends StringConversionPropertyPreferencesAdapter<URI, AspectedObjectProperty<URI>>
  {
    private ApiBaseDebugPropertyPreferencesAdapter(
      AspectedObjectProperty<URI> property )
    {
      super(property, TranscriptionService.class, UriStringConverter.INSTANCE);
    }


    @Override
    public boolean load()
    {
      String sValue = preferences.get(key, null);
      if (sValue != null)
      {
        URI value = converter.fromString(sValue);
        if (value != null)
        {
          super.logger.log(Level.CONFIG,
            "Simulate loading {0}/{1} into \"{2}\": {3} ({4})",
            new Object[]{ preferences.absolutePath(), key,
              property.getName(), value, value.getClass().getName() });
          return true;
        }
      }
      return false;
    }


    @Override
    public void save()
    {
      URI value = property.getValue();
      if (value != null)
      {
        String sValue = converter.toString(value);
        super.logger.log(Level.CONFIG,
          "Simulate saving \"{2}\" into {0}/{1}: {3}",
          new Object[]{ preferences.absolutePath(), key,
            property.getName(), sValue });
      }
    }
  }
}
