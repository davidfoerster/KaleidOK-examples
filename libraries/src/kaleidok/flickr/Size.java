package kaleidok.flickr;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;


public class Size
{
  public Label label;

  public int width, height;

  public String source;


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


    public static Label parse( String s )
    {
      StringBuilder transformed = null;
      int p, offset = 0;
      while ((p = s.indexOf(' ', offset)) >= 0)
      {
        if (transformed == null)
          transformed = new StringBuilder(s.length() - 1);

        if (p + 1 >= s.length())
          throw new IllegalArgumentException(s);

        char c = s.charAt(p + 1);
        switch (Character.getType(c))
        {
        case Character.LOWERCASE_LETTER:
          c = toUpperCase(c);

        case Character.UPPERCASE_LETTER:
        case Character.DECIMAL_DIGIT_NUMBER:
          transformed.append(s, offset, p).append(c);
          break;

        default:
          throw new IllegalArgumentException(s);
        }
        offset = p + 2;
      }
      return valueOf(
        (transformed != null) ?
          transformed.append(s, offset, s.length()).toString() :
          s);
    }


    private static char toUpperCase( char c )
    {
      return (char)(c & ~('A' ^ 'a'));
    }


    public static class Deserializer implements JsonDeserializer<Label>
    {
      public static Deserializer INSTANCE = new Deserializer();

      protected Deserializer() { }

      @Override
      public Label deserialize( JsonElement jsonElement, Type type,
        JsonDeserializationContext context ) throws JsonParseException
      {
        try {
          return parse(jsonElement.getAsString());
        } catch (IllegalArgumentException ex) {
          throw new JsonParseException(ex);
        }
      }
    }
  }
}
