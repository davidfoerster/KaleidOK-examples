package kaleidok.chromatik.data;

import com.google.gson.*;

import java.lang.reflect.Type;


public class ChromatikResponseDeserializer implements JsonDeserializer
{
  public static final ChromatikResponseDeserializer INSTANCE =
    new ChromatikResponseDeserializer();

  protected ChromatikResponseDeserializer() { }

  @Override
  public Object deserialize( JsonElement jsonElement, Type type,
    JsonDeserializationContext context ) throws JsonParseException
  {
    assert type instanceof Class && ChromatikResponse.class.isAssignableFrom((Class<?>) type);

    JsonArray a = jsonElement.getAsJsonArray();
    ChromatikResponse response = new ChromatikResponse();
    response.hits = a.get(0).getAsInt();
    final ChromatikResponse.Result[] results =
      new ChromatikResponse.Result[a.size() - 1];
    for (int i = 0; i < results.length; i++) {
      results[i] =
        context.deserialize(a.get(i + 1), ChromatikResponse.Result.class);
    }
    response.results = results;
    return response;
  }
}
