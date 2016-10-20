package kaleidok.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static kaleidok.util.AssertionUtils.fastAssert;


public final class Reflection
{
  private Reflection() { }


  public static final Collection<Class<?>> WRAPPER_TYPES =
    Arrays.asImmutableList(
      Boolean.class, Character.class, Byte.class, Short.class,
      Integer.class, Long.class, Float.class, Double.class);


  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
    WRAPPER_TYPES.stream().collect(
      IdentityHashMap::new,
      ( map, wrapper ) -> {
        Class<?> primitive;
        try
        {
          primitive = (Class<?>) wrapper.getDeclaredField("TYPE").get(null);
        }
        catch (@SuppressWarnings("ProhibitedExceptionCaught")
          NoSuchFieldException | IllegalAccessException | ClassCastException | NullPointerException ex)
        {
          throw new AssertionError(ex);
        }
        fastAssert(primitive.isPrimitive());

        map.put(wrapper, primitive);
        map.put(primitive, wrapper);
      },
      Map::putAll);


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


  public static Class<?> getTopLevelClass( Class<?> clazz )
  {
    Class<?> enclosingClass;
    while ((enclosingClass = clazz.getEnclosingClass()) != null)
      clazz = enclosingClass;
    return clazz;
  }
}
