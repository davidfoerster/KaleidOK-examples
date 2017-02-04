package kaleidok.net.http.responsehandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.io.Reader;


public class JsonElementResponseHandler extends ReaderSource
  implements ResponseHandler<JsonElement>
{
  private static JsonParser defaultJsonParser = null;

  public static JsonParser getDefaultJsonParser()
  {
    if (defaultJsonParser == null)
      defaultJsonParser = new JsonParser();
    return defaultJsonParser;
  }


  public static final JsonElementResponseHandler DEFAULT_INSTANCE =
    new JsonElementResponseHandler(ReaderResponseHandler.UTF8_HANDLER);


  private final JsonParser jsonParser;


  public JsonElementResponseHandler( ResponseHandler<Reader> readerSource )
  {
    this(readerSource, getDefaultJsonParser());
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
    httpResponse = JsonMimeTypeChecker.INSTANCE.handleResponse(httpResponse);
    try (Reader in = getReader(httpResponse)) {
      return jsonParser.parse(in);
    }
  }
}
