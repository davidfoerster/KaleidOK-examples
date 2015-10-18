package kaleidok.google.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


public final class TypeAdapterManager
{
  private TypeAdapterManager() { }


  public static void registerTypeAdapter( Type type, Object typeAdapter )
  {
    TypeAdapterMap.INSTANCE.put(type, typeAdapter);
  }


  public static Gson getGson()
  {
    return TypeAdapterMap.INSTANCE.getGson();
  }


  public static class TypeAdapterMap extends HashMap<Type, Object>
  {
    private Gson gson = null;


    protected TypeAdapterMap() { }

    static final TypeAdapterMap INSTANCE = new TypeAdapterMap();


    public Gson getGson()
    {
      if (gson == null) {
        synchronized (this) {
          if (gson == null) {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            for (Map.Entry<Type, Object> e: entrySet())
              gsonBuilder.registerTypeAdapter(e.getKey(), e.getValue());
            gson = gsonBuilder.create();
          }
        }
      }
      return gson;
    }


    @Override
    public Object put( Type key, Object value )
    {
      if (key == null)
        throw new NullPointerException();
      if (!isTypeAdapterClass(value.getClass())) {
        throw new IllegalArgumentException(
          "Illegal type adapter class: " + value.getClass().getName());
      }
      Object prev;
      synchronized (this) {
        prev = super.put(key, value);
        if (prev != value)
          gson = null;
      }
      return prev;
    }


    private static final Class<?>[] typeAdapterClasses = {
        JsonDeserializer.class, JsonSerializer.class,
        TypeAdapter.class, InstanceCreator.class
      };

    public static boolean isTypeAdapterClass( Class<?> clazz )
    {
      for (Class<?> tac : typeAdapterClasses) {
        if (tac.isAssignableFrom(clazz))
          return true;
      }
      return false;
    }
  }
}
