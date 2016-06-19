package kaleidok.util;


import java.applet.Applet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


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


  public static double parseDouble( String s, double defaultValue )
    throws NumberFormatException
  {
    return (s != null && !s.isEmpty()) ?
      Double.parseDouble(s) :
      defaultValue;
  }

  public static double parseDouble( Applet a, String name, double defaultValue )
    throws NumberFormatException
  {
    return parseDouble(a.getParameter(name), defaultValue);
  }


  public static boolean parseBoolean( String s )
    throws IllegalArgumentException
  {
    Throwable cause = null;

    if (s != null && !s.isEmpty()) {
      if (Character.isDigit(s.charAt(0))) {
        try {
          return Integer.parseInt(s) != 0;
        } catch (NumberFormatException ex) {
          cause = ex;
        }
      } else {
        Boolean result = BOOLEAN_WORDS.get(s.toLowerCase());
        if (result != null)
          return result;
      }
    }

    String msg = "Not a boolean: " + s;
    throw (cause != null) ?
      new IllegalArgumentException(msg, cause) :
      new IllegalArgumentException(msg);
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
    Class<?> wrapperClass = getWrapperType(targetClass);
    if (wrapperClass == Character.class) {
      if (s.length() != 1) {
        throw new IllegalArgumentException(
          "Cannot cast string of length " + s.length() + " to " +
            targetClass.getName());
      }
      //noinspection unchecked
      return (T) Character.valueOf(s.charAt(0));
    }
    try {
      Method m = wrapperClass.getMethod("valueOf", valueOfParameterTypes);
      if (!wrapperClass.isAssignableFrom(m.getReturnType())) {
        //noinspection ThrowCaughtLocally
        throw new ClassCastException(String.format(
          "Cannot assign return type %s of %s#%s(%s) to %s",
          m.getReturnType().getName(),
          m.getDeclaringClass().getCanonicalName(), m.getName(),
          valueOfParameterTypes[0].getCanonicalName(), wrapperClass.getName()));
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
    if (clazz.isAssignableFrom(String.class)) {
      //noinspection unchecked
      return (s != null) ? (T) s : defaultValue;
    }
    if (s != null && !s.isEmpty()) {
      try {
        T value = valueOf(s, clazz);
        if (value != null)
          return value;
      } catch (IllegalArgumentException ignored) {
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


  private static final Map<String, Boolean> BOOLEAN_WORDS =
    new HashMap<String, Boolean>(16) {{
      put("true", TRUE);
      put("false", FALSE);
      put("yes", TRUE);
      put("no", FALSE);
      put("on", TRUE);
      put("off", FALSE);
      put("enabled", TRUE);
      put("disabled", FALSE);
    }};
}
