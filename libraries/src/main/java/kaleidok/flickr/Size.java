package kaleidok.flickr;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import kaleidok.util.Strings;

import java.io.Serializable;
import java.lang.reflect.Type;


public class Size implements Serializable
{
  private static final long serialVersionUID = 6935510737803562177L;

  @Expose
  public Label label;

  @Expose
  public int width, height;

  @Expose
  public String source;


  public Label getLabel()
  {
    return label;
  }


  @Override
  public String toString()
  {
    return label.name() + '[' + width + 'x' + height + ']';
  }


  public enum Label
  {
    Square,
    LargeSquare,
    Thumbnail,
    Small,
    Small320,
    Medium,
    Medium640,
    Medium800,
    Large,
    Large1600,
    Large2048,
    Original;


    public static Label deserialize( JsonElement jsonElement, Type type,
      JsonDeserializationContext context )
      throws JsonParseException
    {
      assert type instanceof Class && Label.class.isAssignableFrom((Class<?>) type);
      try
      {
        return valueOf(
          Strings.toCamelCase(jsonElement.getAsString()).toString());
      }
      catch (IllegalArgumentException | ClassCastException | IllegalStateException ex)
      {
        throw new JsonParseException(ex);
      }
    }
  }
}
