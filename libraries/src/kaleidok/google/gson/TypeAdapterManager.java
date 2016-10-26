package kaleidok.google.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public final class TypeAdapterManager
{
  private TypeAdapterManager() { }


  public static void registerTypeAdapter( Type type,
    JsonDeserializer<?> typeAdapter )
  {
    TypeAdapterMap.INSTANCE.put(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    JsonSerializer<?> typeAdapter )
  {
    TypeAdapterMap.INSTANCE.put(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    TypeAdapter<?> typeAdapter )
  {
    TypeAdapterMap.INSTANCE.put(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    InstanceCreator<?> typeAdapter )
  {
    TypeAdapterMap.INSTANCE.put(type, typeAdapter);
  }


  public static Gson getGson()
  {
    return TypeAdapterMap.INSTANCE.getGson();
  }


  public static class TypeAdapterMap
  {
    private final Map<Type, Object> map = new HashMap<>();

    private volatile Gson gson = null;


    protected TypeAdapterMap() { }

    static final TypeAdapterMap INSTANCE = new TypeAdapterMap();


    public Gson getGson()
    {
      Gson gson;
      if ((gson = this.gson) == null)
      {
        synchronized (this)
        {
          if ((gson = this.gson) == null)
          {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            for (Map.Entry<Type, Object> e: map.entrySet())
              gsonBuilder.registerTypeAdapter(e.getKey(), e.getValue());
            this.gson = gson = gsonBuilder.create();
          }
        }
      }
      return gson;
    }


    public Object put( Type key, JsonDeserializer<?> value )
    {
      return put(key, (Object) value);
    }


    public Object put( Type key, JsonSerializer<?> value )
    {
      return put(key, (Object) value);
    }


    public Object put( Type key, TypeAdapter<?> value )
    {
      return put(key, (Object) value);
    }


    public Object put( Type key, InstanceCreator<?> value )
    {
      return put(key, (Object) value);
    }


    private synchronized Object put( Type key, Object value )
    {
      Object prev = map.put(
        Objects.requireNonNull(key, "key"),
        Objects.requireNonNull(value, "value"));
      if (!Objects.equals(prev, value))
        gson = null;
      return prev;
    }
  }
}
