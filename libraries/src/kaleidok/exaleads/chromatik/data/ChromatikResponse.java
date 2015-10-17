package kaleidok.exaleads.chromatik.data;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import kaleidok.flickr.Photo;

import java.lang.reflect.Type;


public class ChromatikResponse
{
  public int hits;

  public Result[] results;

  public static class Result
  {
    @Expose
    public int ind;

    @Expose
    public String id, title;

    @Expose
    public String[] tags;

    @Expose
    public int width, height;

    @Expose
    public String thumbnailurl, squarethumbnailurl;

    public Photo flickrPhoto;
  }


  public static class Deserializer implements JsonDeserializer<ChromatikResponse>
  {
    public static final Deserializer INSTANCE = new Deserializer();

    protected Deserializer() { }

    @Override
    public ChromatikResponse deserialize( JsonElement jsonElement, Type type,
      JsonDeserializationContext context ) throws JsonParseException
    {
      assert type instanceof Class && ChromatikResponse.class.isAssignableFrom((Class<?>) type) :
        type + " is not a subclass of " + ChromatikResponse.class.getCanonicalName();

      JsonArray a = jsonElement.getAsJsonArray();
      ChromatikResponse response = new ChromatikResponse();
      response.hits = a.get(0).getAsInt();
      final Result[] results =
        new Result[a.size() - 1];
      for (int i = 0; i < results.length; i++) {
        results[i] =
          context.deserialize(a.get(i + 1), Result.class);
      }
      response.results = results;
      return response;
    }
  }
}
