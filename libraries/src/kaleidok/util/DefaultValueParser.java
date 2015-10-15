package kaleidok.util;


import java.applet.Applet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public final class DefaultValueParser
{
  private DefaultValueParser() { }


  public static int parseInt( String s, int defaultValue )
    throws NumberFormatException
  {
    return (s != null && !s.isEmpty()) ?
      Integer.parseInt(s) :
      defaultValue;
  }

  public static int parseInt( Applet a, String name, int defaultValue )
    throws NumberFormatException
  {
    return parseInt(a.getParameter(name), defaultValue);
  }


  public static long parseLong( String s, long defaultValue )
    throws NumberFormatException
  {
    return (s != null && !s.isEmpty()) ?
      Long.parseLong(s) :
      defaultValue;
  }

  public static long parseLong( Applet a, String name, long defaultValue )
    throws NumberFormatException
  {
    return parseLong(a.getParameter(name), defaultValue);
  }


  private static final String[] BOOLEAN_WORDS = {
      "true", "false", "yes", "no", "on", "off", "enabled", "disabled"
    };

  public static boolean parseBoolean( String s )
    throws IllegalArgumentException
  {
    if (s != null && !s.isEmpty()) {
      if (Character.isDigit(s.charAt(0))) {
        try {
          return Integer.parseInt(s) != 0;
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException("Not a boolean: " + s, ex);
        }
      }
      for (int i = 0; i < BOOLEAN_WORDS.length; i++) {
        if (BOOLEAN_WORDS[i].equalsIgnoreCase(s))
          return i % 2 == 0;
      }
    }
    throw new IllegalArgumentException("Not a boolean: " + s);
  }

  public static boolean parseBoolean( String s, boolean defaultValue )
    throws IllegalArgumentException
  {
    return (s != null && !s.isEmpty()) ?
      parseBoolean(s) :
      defaultValue;
  }

  public static boolean parseBoolean( Applet a, String name, boolean defaultValue )
    throws IllegalArgumentException
  {
    return parseBoolean(a.getParameter(name), defaultValue);
  }


  private static final Class<?>[] valueOfParameterTypes = { String.class };

  public static <T> T valueOf( String s, Class<T> targetClass )
    throws IllegalArgumentException
  {
    try {
      Method m = targetClass.getMethod("valueOf", valueOfParameterTypes);
      if (!targetClass.isAssignableFrom(m.getReturnType())) {
        throw new ClassCastException(String.format(
          "Cannot assign return type %3$s of %4$s#%1$s(%2$s) to %4$s",
          m.getName(), valueOfParameterTypes[0].getSimpleName(),
          m.getReturnType().getName(), targetClass.getCanonicalName()));
      }
      //noinspection unchecked
      return (T) m.invoke(null, s);
    } catch (NoSuchMethodException | IllegalAccessException | ClassCastException | IllegalArgumentException ex) {
      throw new IllegalArgumentException("Cannot parse to " + targetClass.getName(), ex);
    } catch (InvocationTargetException ex) {
      throw new IllegalArgumentException(ex.getCause());
    }
  }


  public static <T> T parse( String s, T defaultValue )
    throws IllegalArgumentException
  {
    //noinspection unchecked
    return parse(s, defaultValue, (Class<T>) defaultValue.getClass());
  }

  public static <T> T parse( String s, T defaultValue, Class<T> clazz )
    throws IllegalArgumentException
  {
    if (clazz == String.class || clazz == CharSequence.class) {
      //noinspection unchecked
      return (s != null) ? (T) s : defaultValue;
    }
    if (s != null && !s.isEmpty()) {
      try {
        T value = valueOf(s, clazz);
        if (value != null)
          return value;
      } catch (IllegalArgumentException ex) {
        // go to default
      }
    }
    return defaultValue;
  }


  public static Class<?> getWrapperType( Class<?> clazz )
  {
    return clazz.isPrimitive() ? PRIMITIVES_TO_WRAPPERS.get(clazz) : clazz;
  }

  public static final Collection<Class<?>> WRAPPER_TYPES =
    Arrays.asImmutableList(new Class<?>[]{
      Boolean.class, Character.class, Byte.class, Short.class,
      Integer.class, Long.class, Float.class, Double.class
    });

  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
    new HashMap<Class<?>, Class<?>>(16) {{
      for (Class<?> clazz: WRAPPER_TYPES) {
        try {
          put((Class<?>) clazz.getDeclaredField("TYPE").get(null), clazz);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
          throw new AssertionError(ex);
        }
      }
    }};
}
