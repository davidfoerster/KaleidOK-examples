package kaleidok.flickr.internal;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import kaleidok.flickr.FlickrException;


public abstract class FlickrResponseDeserializer<T>
  implements JsonDeserializer<T>
{
  protected static JsonObject unwrap( JsonElement jsonElement, String which )
  {
    if (!jsonElement.isJsonObject())
      throw new JsonParseException("JSON object expected");
    JsonObject o = jsonElement.getAsJsonObject();

    String stat = o.getAsJsonPrimitive("stat").getAsString();
    switch (stat) {
    case "ok":
      return o.getAsJsonObject(which);

    case "fail":
      throw new JsonParseException(new FlickrException(
        o.getAsJsonPrimitive("message").getAsString(),
        o.getAsJsonPrimitive("code").getAsInt(), null));

    default:
      throw new JsonParseException(
        "Invalid status code: " + String.valueOf(stat));
    }
  }
}
