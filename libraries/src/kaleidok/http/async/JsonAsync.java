package kaleidok.http.async;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kaleidok.http.responsehandler.JsonElementResponseHandler;
import kaleidok.http.responsehandler.JsonResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;

import static kaleidok.http.responsehandler.JsonMimeTypeChecker.MIME_TYPE_MAP;


public class JsonAsync extends AsyncBase
{
  public Gson gson = JsonResponseHandler.getDefaultGson();


  public JsonAsync()
  {
    super(MIME_TYPE_MAP);
  }

  @Override
  public JsonAsync use( org.apache.http.client.fluent.Executor executor )
  {
    super.use(executor);
    return this;
  }

  @Override
  public JsonAsync use( java.util.concurrent.Executor concurrentExec )
  {
    super.use(concurrentExec);
    return this;
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
    Gson gson = this.gson;
    JsonResponseHandler<?> lrh = lastResponseHandler;

    if (lrh == null || lrh.targetClass != clazz ||
      lrh.gson != gson)
    {
      lrh = new JsonResponseHandler<>(clazz, gson);
      lastResponseHandler = lrh;
    }
    return (JsonResponseHandler<T>) lrh;
  }

  private JsonResponseHandler<?> lastResponseHandler = null;
}
