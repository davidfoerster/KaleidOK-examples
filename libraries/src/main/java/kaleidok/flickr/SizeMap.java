package kaleidok.flickr;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("SpellCheckingInspection")
public class SizeMap extends TreeMap<Size.Label, Size>
{
  private static final long serialVersionUID = -8966551070440870922L;

  @Expose
  public boolean canblog, canprint, candownload;


  public SizeMap() { }


  public SizeMap( Map<Size.Label, ? extends Size> m )
  {
    super(m);
  }


  public static SizeMap deserialize( JsonElement jsonElement, Type type,
    JsonDeserializationContext context )
    throws JsonParseException
  {
    assert type instanceof Class && SizeMap.class.isAssignableFrom((Class<?>) type);

    JsonObject o = Flickr.unwrap(jsonElement, "sizes");
    SizeMap sizes = new SizeMap(
      Stream.<Size>of(context.deserialize(o.get("size"), Size[].class))
        .collect(Collectors.toMap(Size::getLabel, Function.identity())));

    sizes.canblog = o.getAsJsonPrimitive("canblog").getAsInt() != 0;
    sizes.canprint = o.getAsJsonPrimitive("canprint").getAsInt() != 0;
    sizes.candownload = o.getAsJsonPrimitive("candownload").getAsInt() != 0;

    return sizes;
  }
}
