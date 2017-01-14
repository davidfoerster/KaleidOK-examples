package kaleidok.google.gson;

import com.google.gson.*;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public final class TypeAdapterManager
{
  private TypeAdapterManager() { }


  public static void registerTypeAdapter( Type type,
    JsonDeserializer<?> typeAdapter )
  {
    TypeAdapterSet.INSTANCE.add(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    JsonSerializer<?> typeAdapter )
  {
    TypeAdapterSet.INSTANCE.add(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    TypeAdapter<?> typeAdapter )
  {
    TypeAdapterSet.INSTANCE.add(type, typeAdapter);
  }


  public static void registerTypeAdapter( Type type,
    InstanceCreator<?> typeAdapter )
  {
    TypeAdapterSet.INSTANCE.add(type, typeAdapter);
  }


  public static Gson getGson()
  {
    return TypeAdapterSet.INSTANCE.getGson();
  }


  public static class TypeAdapterSet
  {
    private final Set<Pair<Type, ?>> adapters = new HashSet<>();

    private volatile Gson gson = null;


    protected TypeAdapterSet() { }

    static final TypeAdapterSet INSTANCE = new TypeAdapterSet();


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
            for (Pair<Type, ?> e: adapters)
              gsonBuilder.registerTypeAdapter(e.getKey(), e.getValue());
            this.gson = gson = gsonBuilder.create();
          }
        }
      }
      return gson;
    }


    public boolean add( Type key, JsonDeserializer<?> value )
    {
      return add(key, (Object) value);
    }


    public boolean add( Type key, JsonSerializer<?> value )
    {
      return add(key, (Object) value);
    }


    public boolean add( Type key, TypeAdapter<?> value )
    {
      return add(key, (Object) value);
    }


    public boolean add( Type key, InstanceCreator<?> value )
    {
      return add(key, (Object) value);
    }


    private synchronized boolean add( Type key, Object value )
    {
      boolean changed = adapters.add(Pair.of(
        Objects.requireNonNull(key, "key"),
        Objects.requireNonNull(value, "value")));
      if (changed)
        gson = null;
      return changed;
    }
  }
}
