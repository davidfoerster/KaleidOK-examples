package kaleidok.util;


import java.applet.Applet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class DefaultValueParser
{
  private DefaultValueParser() { }


  public static int parseInt( String s, int defaultValue )
  {
    return (s != null && !s.isEmpty()) ?
      Integer.parseInt(s) :
      defaultValue;
  }

  public static int parseInt( Applet a, String name, int defaultValue )
  {
    return parseInt(a.getParameter(name), defaultValue);
  }


  public static boolean parseBoolean( String s, boolean defaultValue )
  {
    return (s != null && !s.isEmpty()) ?
      Boolean.parseBoolean(s) :
      defaultValue;
  }

  public static boolean parseBoolean( Applet a, String name, boolean defaultValue )
  {
    return parseBoolean(a.getParameter(name), defaultValue);
  }


  private static final Class<?>[] valueOfParameterTypes =
    new Class<?>[]{ String.class };

  public static <T> T valueOf( String s, Class<T> targetClass )
    throws IllegalArgumentException
  {
    try {
      Method m = targetClass.getMethod("valueOf", valueOfParameterTypes);
      if (targetClass.isAssignableFrom(m.getReturnType())) {
        try {
          //noinspection unchecked
          return (T) m.invoke(null, s);
        } catch (InvocationTargetException ex) {
          throw new IllegalArgumentException(ex.getCause());
        }
      }
      throw new ClassCastException(
        "Cannot assign return type " + m.getReturnType().getName() + " to " + targetClass.getName());
    }
    catch (NoSuchMethodException | IllegalAccessException | ClassCastException | NullPointerException ex) {
      throw new Error(
        targetClass.getName() + " cannot be created with #valueOf(String)", ex);
    }
  }

  public static Object parse( String s, Object defaultValue )
  {
    if (defaultValue instanceof String)
      return (s != null) ? s : defaultValue;
    if (s != null && !s.isEmpty()) {
      try {
        Object value = valueOf(s, defaultValue.getClass());
        if (value != null)
          return value;
      } catch (IllegalArgumentException ex) {
        // go to default
      }
    }
    return defaultValue;
  }
}
