package kaleidok.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import static kaleidok.util.AssertionUtils.fastAssert;


public final class Reflection
{
  private Reflection() { }


  public static final Collection<Class<?>> WRAPPER_TYPES =
    Arrays.asImmutableList(
      Boolean.class, Character.class, Byte.class, Short.class,
      Integer.class, Long.class, Float.class, Double.class);


  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
    new IdentityHashMap<Class<?>, Class<?>>(WRAPPER_TYPES.size() * 3) {{
      for (Class<?> clazz: WRAPPER_TYPES)
      {
        Class<?> primitive;
        try
        {
          primitive = (Class<?>) clazz.getDeclaredField("TYPE").get(null);
          fastAssert(primitive.isPrimitive());
        }
        catch (@SuppressWarnings("ProhibitedExceptionCaught")
          NoSuchFieldException | IllegalAccessException | ClassCastException | NullPointerException ex)
        {
          throw new AssertionError(ex);
        }
        put(primitive, clazz);
        put(clazz, primitive);
      }
    }};


  public static Class<?> getWrapperType( Class<?> clazz )
  {
    return clazz.isPrimitive() ? PRIMITIVES_TO_WRAPPERS.get(clazz) : clazz;
  }


  public static Class<?> getPrimitiveType( Class<?> clazz )
  {
    return clazz.isPrimitive() ? clazz : PRIMITIVES_TO_WRAPPERS.get(clazz);
  }


  public static <T> Type[] getTypeArguments( Class<? extends T> clazz,
    Class<T> baseClass )
  {
    if (clazz == baseClass)
      return null;

    if (!baseClass.isAssignableFrom(clazz))
    {
      throw new IllegalArgumentException(
        clazz.getName() + " doesn't derive from " + baseClass.getName());
    }

    Type type = clazz.getGenericSuperclass();
    while (!(type instanceof ParameterizedType) ||
      ((ParameterizedType) type).getRawType() != baseClass)
    {
      type = (type instanceof ParameterizedType) ?
        ((Class<?>) ((ParameterizedType) type).getRawType())
          .getGenericSuperclass() :
        ((Class<?>) type).getGenericSuperclass();
    }

    return ((ParameterizedType) type).getActualTypeArguments();
  }
}
