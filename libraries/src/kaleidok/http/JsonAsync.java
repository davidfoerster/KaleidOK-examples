package kaleidok.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Future;

import static kaleidok.http.JsonHttpConnection.MIME_TYPE_MAP;


public class JsonAsync extends AsyncBase
{
  public Gson gson = JsonHttpConnection.getGson();


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

  protected static final ResponseMimeTypeChecker jsonMimeTypeChecker =
    new ResponseMimeTypeChecker(MIME_TYPE_MAP);


  protected static class ReaderSource
  {
    public final ResponseHandler<Reader> readerSource;

    public ReaderSource( ResponseHandler<Reader> readerSource )
    {
      this.readerSource = readerSource;
    }

    protected Reader getReader( HttpResponse httpResponse ) throws IOException
    {
      return readerSource.handleResponse(
        jsonMimeTypeChecker.handleResponse(httpResponse));
    }
  }


  public static class JsonElementResponseHandler extends ReaderSource
    implements ResponseHandler<JsonElement>
  {
    public static final JsonElementResponseHandler DEFAULT_INSTANCE =
      new JsonElementResponseHandler(ReaderResponseHandler.UTF8_HANDLER);

    private final JsonParser jsonParser;

    public JsonElementResponseHandler( ResponseHandler<Reader> readerSource )
    {
      this(readerSource, JsonHttpConnection.getJsonParser());
    }

    public JsonElementResponseHandler( ResponseHandler<Reader> readerSource,
      JsonParser jsonParser )
    {
      super(readerSource);
      this.jsonParser = jsonParser;
    }

    @Override
    public JsonElement handleResponse( HttpResponse httpResponse )
      throws IOException
    {
      try (Reader in = getReader(httpResponse)) {
        return jsonParser.parse(in);
      }
    }
  }


  public static class JsonResponseHandler<T> extends ReaderSource
    implements ResponseHandler<T>
  {
    public final Gson gson;

    public final Class<? extends T> targetClass;

    public JsonResponseHandler( Class<? extends T> targetClass )
    {
      this(targetClass, JsonHttpConnection.getGson());
    }

    public JsonResponseHandler( Class<? extends T> targetClass, Gson gson )
    {
      this(targetClass, gson,
        JsonElementResponseHandler.DEFAULT_INSTANCE.readerSource);
    }

    public JsonResponseHandler( Class<? extends T> targetClass,
      Gson gson, ResponseHandler<Reader> readerSource )
    {
      super(readerSource);
      this.gson = gson;
      this.targetClass = targetClass;
    }

    @Override
    public T handleResponse( HttpResponse httpResponse ) throws IOException
    {
      try (Reader in = getReader(httpResponse)) {
        return gson.fromJson(in, targetClass);
      }
    }
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
