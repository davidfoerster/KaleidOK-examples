package kaleidok.exaleads.chromatik.data;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import kaleidok.flickr.Photo;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.stream.StreamSupport;


public class ChromatikResponse implements Serializable
{
  private static final long serialVersionUID = 198977843581142016L;

  public int hits;

  public Result[] results;


  public static class Result implements Serializable
  {
    private static final long serialVersionUID = 7448038875557807767L;

    @Expose
    public int ind;

    @Expose
    public String id, title;

    @Expose
    public String[] tags;

    @Expose
    public int width, height;

    @SuppressWarnings("SpellCheckingInspection")
    @Expose
    public String thumbnailurl, squarethumbnailurl;

    public Photo flickrPhoto;
  }


  public static ChromatikResponse deserialize( JsonElement jsonElement, Type type,
    final JsonDeserializationContext context )
    throws JsonParseException
  {
    assert type instanceof Class && ChromatikResponse.class.isAssignableFrom((Class<?>) type) :
      type.getTypeName() + " is no subclass of " + ChromatikResponse.class.getCanonicalName();

    JsonArray a = jsonElement.getAsJsonArray();
    ChromatikResponse response = new ChromatikResponse();
    response.hits = a.get(0).getAsInt();
    response.results =
      StreamSupport.stream(a.spliterator(), false).skip(1)
        .map((resultItem) -> context.deserialize(resultItem, Result.class))
        .toArray(Result[]::new);
    return response;
  }
}
