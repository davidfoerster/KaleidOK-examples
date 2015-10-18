package kaleidok.flickr;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import kaleidok.flickr.internal.FlickrResponseDeserializer;

import java.lang.reflect.Type;
import java.util.TreeMap;


public class SizeMap extends TreeMap<Size.Label, Size>
{
  public boolean canblog, canprint, candownload;


  public static class Deserializer extends FlickrResponseDeserializer<SizeMap>
  {
    public static final Deserializer INSTANCE = new Deserializer();

    protected Deserializer() { }

    @Override
    public SizeMap deserialize( JsonElement jsonElement, Type type,
      JsonDeserializationContext context ) throws JsonParseException
    {
      JsonObject o = unwrap(jsonElement, "sizes");
      Size[] a = context.deserialize(o.get("size"), Size[].class);
      SizeMap sizes = new SizeMap();
      sizes.canblog = o.getAsJsonPrimitive("canblog").getAsInt() != 0;
      sizes.canprint = o.getAsJsonPrimitive("canprint").getAsInt() != 0;
      sizes.candownload = o.getAsJsonPrimitive("candownload").getAsInt() != 0;

      for (Size s: a)
        sizes.put(s.label, s);

      return sizes;
    }
  }
}
