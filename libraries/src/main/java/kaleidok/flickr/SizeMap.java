package kaleidok.flickr;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SizeMap extends TreeMap<Size.Label, Size>
{
  private static final long serialVersionUID = -8966551070440870922L;

  @SuppressWarnings("SpellCheckingInspection")
  @Expose
  @SerializedName("canblog")
  public boolean canBlog;

  @SuppressWarnings("SpellCheckingInspection")
  @Expose
  @SerializedName("canprint")
  public boolean canPrint;

  @SuppressWarnings("SpellCheckingInspection")
  @Expose
  @SerializedName("candownload")
  public boolean canDownload;


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

    try
    {
      for (Field f: SizeMap.class.getDeclaredFields())
      {
        Expose expose = f.getAnnotation(Expose.class);
        if (expose != null && expose.deserialize() &&
          !Modifier.isFinal(f.getModifiers()))
        {
          SerializedName serializedName =
            f.getAnnotation(SerializedName.class);
          JsonPrimitive valueElement =
            o.getAsJsonPrimitive(
              (serializedName != null) ? serializedName.value() : f.getName());
          if (valueElement != null)
          {
            assert f.getType() == boolean.class;
            f.setBoolean(sizes,
              valueElement.isBoolean() ?
                valueElement.getAsBoolean() :
                valueElement.getAsInt() != 0);
          }
        }
      }
    }
    catch (IllegalAccessException|NumberFormatException|ClassCastException ex)
    {
      throw new JsonParseException(ex);
    }

    return sizes;
  }
}
