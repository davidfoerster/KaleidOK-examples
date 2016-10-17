package kaleidok.google.speech;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import kaleidok.javafx.beans.property.AspectedObjectProperty;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.util.converter.UriStringConverter;
import kaleidok.util.concurrent.DaemonThreadFactory;
import org.apache.http.concurrent.FutureCallback;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static kaleidok.http.util.URLEncoding.appendEncoded;


public abstract class TranscriptionServiceBase implements PreferenceBean
{
  protected final AspectedObjectProperty<URI> apiBase;

  protected final AspectedStringProperty accessKey;

  protected final AspectedStringProperty language;

  private final ServiceUrlBinding serviceUrl;

  public FutureCallback<SttResponse> resultHandler;


  protected final ExecutorService executor =
    new ThreadPoolExecutor(1, 1, 0, TimeUnit.NANOSECONDS,
      new ArrayBlockingQueue<>(3), executorThreadFactory);

  protected static final ThreadFactory executorThreadFactory =
    new DaemonThreadFactory("Speech transcription", true);


  protected TranscriptionServiceBase( URI apiBase, String accessKey,
    FutureCallback<SttResponse> resultHandler )
  {
    this.apiBase =
      new AspectedObjectProperty<>(this, "API base URI", apiBase);

    this.accessKey =
      new AspectedStringProperty(this, "API access key", accessKey);
    this.accessKey.addAspect(PropertyPreferencesAdapterTag.getInstance());

    language = new AspectedStringProperty(this, "language", "en");
    language.addAspect(PropertyPreferencesAdapterTag.getInstance());

    this.resultHandler = resultHandler;
    this.serviceUrl =
      new ServiceUrlBinding(this.apiBase, this.accessKey, language);
  }


  public abstract ReadOnlyObjectProperty<URI> apiBaseProperty();

  public URI getApiBase()
  {
    return apiBaseProperty().get();
  }

  public abstract void setApiBase( URI apiBase );


  public StringProperty accessKeyProperty()
  {
    return accessKey;
  }

  public String getAccessKey()
  {
    return accessKey.get();
  }

  public void setAccessKey( String accessKey )
  {
    this.accessKey.set(accessKey);
  }


  public StringProperty languageProperty()
  {
    return language;
  }

  public String getLanguage()
  {
    return language.get();
  }

  public void setLanguage( String language )
  {
    this.language.set(language);
  }


  protected ObservableObjectValue<URI> serviceUrlValue()
  {
    return serviceUrl;
  }

  protected URI getServiceUri()
  {
    return serviceUrlValue().get();
  }


  @OverridingMethodsMustInvokeSuper
  public void execute( Transcription task )
  {
    executor.execute(task);
  }


  @OverridingMethodsMustInvokeSuper
  public void shutdownNow()
  {
    executor.shutdownNow();
  }


  boolean isInQueue( Transcription task )
  {
    return executor instanceof ThreadPoolExecutor &&
      ((ThreadPoolExecutor) executor).getQueue().contains(task);
  }


  @Override
  public String getName()
  {
    return "Transcription service";
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.of(apiBase, accessKey, language)
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny)
      .filter(Objects::nonNull);
  }


  private static final class ServiceUrlBinding extends ObjectBinding<URI>
  {
    private final ObservableValue<URI> apiBase;

    private final ObservableStringValue accessKey;

    private final ObservableStringValue language;


    private ServiceUrlBinding( ObservableValue<URI> apiBase,
      ObservableStringValue accessKey, ObservableStringValue language )
    {
      bind(apiBase, accessKey, language);

      this.apiBase = apiBase;
      this.accessKey = accessKey;
      this.language = language;
    }


    private static final String
      URL_SPEC_PREFIX = "recognize?output=json&lang=",
      URL_SPEC_KEY_BIT = "&key=";


    @Override
    protected URI computeValue()
    {
      URI apiBase =
        Objects.requireNonNull(this.apiBase.getValue(), "API base");
      String
        language = Objects.requireNonNull(this.language.get(), "language"),
        accessKey = Objects.requireNonNull(this.accessKey.get(), "access key");

      StringBuilder urlSpec = new StringBuilder(
        URL_SPEC_PREFIX.length() + language.length() +
          URL_SPEC_KEY_BIT.length() + accessKey.length());
      appendEncoded(language, urlSpec.append(URL_SPEC_PREFIX));
      appendEncoded(accessKey, urlSpec.append(URL_SPEC_KEY_BIT));

      try
      {
        return apiBase.resolve(urlSpec.toString());
      }
      catch (IllegalArgumentException ex)
      {
        throw new AssertionError(ex);
      }
    }


    @Override
    public void dispose()
    {
      unbind(apiBase, accessKey, language);
    }
  }
}
