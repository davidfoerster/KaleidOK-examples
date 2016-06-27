package kaleidok.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public final class Reflection
{
  private Reflection() { }


  public static final Collection<Class<?>> WRAPPER_TYPES =
    Arrays.asImmutableList(
      Boolean.class, Character.class, Byte.class, Short.class,
      Integer.class, Long.class, Float.class, Double.class);


  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
    new HashMap<Class<?>, Class<?>>(WRAPPER_TYPES.size() * 3) {{
      for (Class<?> clazz: WRAPPER_TYPES)
      {
        Class<?> primitive;
        try {
          primitive = (Class<?>) clazz.getDeclaredField("TYPE").get(null);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
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
    Class<?> primitive = PRIMITIVES_TO_WRAPPERS.get(clazz);
    return (primitive != null && primitive.isPrimitive()) ? primitive : null;
  }
}
