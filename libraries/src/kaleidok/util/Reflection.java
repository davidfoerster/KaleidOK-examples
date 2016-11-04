package kaleidok.util;

import java.lang.reflect.Array;
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


  public static Class<?> getNamedSuperClass( Class<?> clazz )
  {
    return getNamedSuperClass(clazz, Class.class);
  }

  public static String getAnonymousClassSimpleName( Class<?> clazz )
  {
    return getNamedSuperClass(clazz, String.class);
  }


  private static <R> R getNamedSuperClass( final Class<?> origin,
    Class<R> returnType )
  {
    Class<?> clazz = origin;

    int arrayDepth;
    for (arrayDepth = 0; clazz.isArray(); arrayDepth++)
      clazz = clazz.getComponentType();

    String simpleName = clazz.getSimpleName();
    if (!simpleName.isEmpty())
      return getClassOrSimpleName(origin, simpleName, arrayDepth, returnType);

    Class<?> superClass = clazz.getSuperclass();
    if (superClass == Object.class)
    {
      // Looks like it might be an anonymous interface implementation
      Class<?>[] interfaces = clazz.getInterfaces();
      if (interfaces.length != 0)
        superClass = interfaces[0];
      simpleName = superClass.getSimpleName();
      fastAssert(!simpleName.isEmpty(), "Anonymous interface");
    }
    else
    {
      while (superClass != null)
      {
        simpleName = superClass.getSimpleName();
        if (!simpleName.isEmpty())
          break;
        superClass = superClass.getSuperclass();
      }
      fastAssert(!simpleName.isEmpty(), "Anonymous top-level class");
    }

    return getClassOrSimpleName(origin, simpleName, arrayDepth, returnType);
  }


  private static <R> R getClassOrSimpleName( Class<?> namedClazz,
    String simpleName, int arrayDepth, Class<R> returnType )
  {
    if (returnType == Class.class)
    {
      if (arrayDepth != 0)
      {
        Object templateInstance =
          (arrayDepth == 1) ?
            Array.newInstance(namedClazz, 0) :
            Array.newInstance(namedClazz, new int[arrayDepth]);
        namedClazz = templateInstance.getClass();
      }

      fastAssert(returnType.isInstance(namedClazz));
      //noinspection unchecked
      return (R) namedClazz;
    }

    if (returnType == String.class)
    {
      if (arrayDepth != 0)
      {
        int i = simpleName.length();
        final char[] a = new char[i + arrayDepth * 2];
        simpleName.getChars(0, i, a, 0);
        while (i < a.length)
        {
          a[i++] = '[';
          a[i++] = ']';
        }
        simpleName = String.valueOf(a);
      }

      fastAssert(returnType.isInstance(simpleName));
      //noinspection unchecked
      return (R) simpleName;
    }

    // unsupported return type
    throw new AssertionError();
  }
}
