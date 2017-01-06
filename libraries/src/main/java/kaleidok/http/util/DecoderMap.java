package kaleidok.http.util;

import kaleidok.util.containers.FreezableMap;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;


public class DecoderMap
  extends FreezableMap<String, Constructor<? extends FilterInputStream>>
{
  private static final Class<?>[] decoderConstructorParams = {
    InputStream.class
  };


  public DecoderMap()
  {
    super(new HashMap<>());
  }


  public DecoderMap( int initialCapacity )
  {
    super(new HashMap<>(initialCapacity));
  }


  public DecoderMap( DecoderMap o )
  {
    //noinspection unchecked,OverlyStrongTypeCast
    super((HashMap<String, Constructor<? extends FilterInputStream>>)
      ((HashMap<String, Constructor<? extends FilterInputStream>>) o.underlying).clone());
  }


  public Constructor<? extends FilterInputStream> put( String key,
    Class<? extends FilterInputStream> value )
  {
    checkFrozen();
    if (value == null)
      return underlying.put(key, null);

    checkClass(value);
    try
    {
      return underlying.put(key, value.getConstructor(decoderConstructorParams));
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalArgumentException(ex);
    }
  }


  @Override
  protected boolean isValidEntry( String key, Constructor<? extends FilterInputStream> value )
  {
    if (value == null)
      return true;

    if (!Modifier.isPublic(value.getModifiers()))
    {
      throw new IllegalArgumentException(new IllegalAccessException(
        value + " isn't public"));
    }
    if (!Arrays.equals(value.getParameters(), decoderConstructorParams))
    {
      throw new IllegalArgumentException(new IllegalArgumentException(
        value + " has an illegal parameter list; expected " +
          Arrays.toString(decoderConstructorParams)));
    }

    checkClass(value.getDeclaringClass());
    return true;
  }


  private static void checkClass( Class<?> clazz )
  {
    if (clazz == null)
      return;
    if (Modifier.isAbstract(clazz.getModifiers()))
    {
      throw new IllegalArgumentException(new InstantiationException(
        clazz.getName() + " is abstract"));
    }
    if (!FilterInputStream.class.isAssignableFrom(clazz))
    {
      throw new IllegalArgumentException(new ClassCastException(
        clazz.getName() + " doesn't extend " +
          FilterInputStream.class.getCanonicalName()));
    }
  }


  @SuppressWarnings("resource")
  public InputStream getDecodedStream( String encoding, InputStream in )
    throws IOException
  {
    Objects.requireNonNull(in);

    Constructor<? extends FilterInputStream> dec = get(encoding);
    if (dec != null)
    {
      try
      {
        in = dec.newInstance(in);
      }
      catch (InvocationTargetException ex)
      {
        Throwable cause = ex.getCause();
        if (cause instanceof IOException)
          throw (IOException) cause;
        throw new Error(cause); // FilterInputStream constructors shouldn't throw anything except IOException
      }
      catch (ReflectiveOperationException ex)
      {
        throw new AssertionError(ex);
      }
    }
    else if (!containsKey(encoding))
    {
      throw new NoSuchElementException(
        "Invalid or unsupported encoding: " + encoding);
    }
    return in;
  }
}
