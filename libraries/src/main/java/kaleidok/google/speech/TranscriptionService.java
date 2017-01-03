package kaleidok.google.speech;

import javafx.beans.property.ObjectProperty;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import org.apache.http.concurrent.FutureCallback;

import java.net.URI;
import java.net.URISyntaxException;


public class TranscriptionService extends TranscriptionServiceBase
{
  public static final URI DEFAULT_API_BASE;

  static
  {
    try {
      DEFAULT_API_BASE =
        new URI("https", "www.google.com", "/speech-api/v2/", null);
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


  public TranscriptionService( String accessKey,
    FutureCallback<SttResponse> resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }


  public TranscriptionService( URI apiBase, String accessKey,
    FutureCallback<SttResponse> resultHandler )
  {
    super(apiBase, accessKey, resultHandler);
    this.apiBase.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  @Override
  public ObjectProperty<URI> apiBaseProperty()
  {
    return apiBase;
  }

  @Override
  public void setApiBase( URI apiBase )
  {
    this.apiBase.set(apiBase);
  }
}
