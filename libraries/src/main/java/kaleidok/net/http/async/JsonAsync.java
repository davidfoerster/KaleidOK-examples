package kaleidok.net.http.async;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kaleidok.net.http.responsehandler.JsonElementResponseHandler;
import kaleidok.net.http.responsehandler.JsonResponseHandler;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.net.http.responsehandler.JsonMimeTypeChecker;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;


public class JsonAsync extends AsyncBase
{
  private Gson gson = null;


  public JsonAsync( org.apache.http.client.fluent.Async fluentAsync )
  {
    super(fluentAsync, JsonMimeTypeChecker.MIME_TYPE_MAP);
  }


  public Gson getGson()
  {
    Gson gson = this.gson;
    return (gson != null) ? gson : TypeAdapterManager.getGson();
  }

  public void setGson( Gson gson )
  {
    this.gson = gson;
  }


  @Override
  public Future<JsonElement> execute( Request request )
  {
    return execute(request, (FutureCallback<JsonElement>) null);
  }

  public Future<JsonElement> execute( Request request,
    FutureCallback<JsonElement> callback )
  {
    applyAcceptedMimeTypes(request);
    return underlying.execute(request,
      JsonElementResponseHandler.DEFAULT_INSTANCE, callback);
  }

  public <T> Future<T> execute( Request request, Class<T> clazz )
  {
    return execute(request, clazz, null);
  }

  public <T> Future<T> execute( Request request, Class<T> clazz,
    FutureCallback<T> callback )
  {
    applyAcceptedMimeTypes(request);
    return underlying.execute(request, getJsonResponseHandler(clazz),
      callback);
  }


  private <T> JsonResponseHandler<T> getJsonResponseHandler( Class<T> clazz )
  {
    Gson gson = getGson();
    JsonResponseHandler<?> lrh = lastResponseHandler;

    if (lrh == null || lrh.targetClass != clazz ||
      lrh.getGson() != gson)
    {
      lrh = new JsonResponseHandler<>(clazz, gson);
      lastResponseHandler = lrh;
    }

    //noinspection unchecked
    return (JsonResponseHandler<T>) lrh;
  }

  private JsonResponseHandler<?> lastResponseHandler = null;
}
