package kaleidok.http.responsehandler;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.io.Reader;


public class JsonResponseHandler<T> extends ReaderSource
  implements ResponseHandler<T>
{
  private static Gson defaultGson = null;

  public static Gson getDefaultGson()
  {
    if (defaultGson == null)
      defaultGson = new Gson();
    return defaultGson;
  }


  public final Gson gson;

  public final Class<? extends T> targetClass;


  public JsonResponseHandler( Class<? extends T> targetClass )
  {
    this(targetClass, getDefaultGson());
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
