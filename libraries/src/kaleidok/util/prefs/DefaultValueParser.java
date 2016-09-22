package kaleidok.util.prefs;


import kaleidok.util.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


public final class DefaultValueParser
{
  private DefaultValueParser() { }


  public static int parseInt( String s, int defaultValue )
    throws NumberFormatException
  {
    return (s != null) ? Integer.parseInt(s) : defaultValue;
  }


  public static long parseLong( String s, long defaultValue )
    throws NumberFormatException
  {
    return (s != null) ? Long.parseLong(s) : defaultValue;
  }


  public static double parseDouble( String s, double defaultValue )
    throws NumberFormatException
  {
    return (s != null) ? Double.parseDouble(s) : defaultValue;
  }


  public static boolean parseBoolean( String s )
    throws IllegalArgumentException
  {
    Objects.requireNonNull(s);
    Throwable cause = null;

    if (!s.isEmpty()) {
      if (Character.isDigit(s.charAt(0))) {
        try {
          return Integer.parseInt(s) != 0;
        } catch (NumberFormatException ex) {
          cause = ex;
        }
      } else {
        Boolean result = BOOLEAN_WORDS.get(s.toLowerCase(Locale.ROOT));
        if (result != null)
          return result;
      }
    }

    throw new IllegalArgumentException("Not a boolean: " + s, cause);
  }


  public static boolean parseBoolean( String s, boolean defaultValue )
    throws IllegalArgumentException
  {
    return (s != null) ? parseBoolean(s) : defaultValue;
  }


  private static final Class<?>[] valueOfParameterTypes = { String.class };


  @SuppressWarnings("unchecked")
  public static <T> T valueOf( String s, Class<T> targetClass )
    throws IllegalArgumentException
  {
    Class<?> wrapperClass = Reflection.getWrapperType(targetClass);
    Throwable ex;
    if (wrapperClass == Character.class)
    {
      if (s.length() == 1)
        return (T) Character.valueOf(s.charAt(0));

      ex = new IllegalArgumentException(
        "Cannot cast string of length " + s.length() + " to " +
          targetClass.getName());
    }
    else try
    {
      Method m = wrapperClass.getMethod("valueOf", valueOfParameterTypes);
      if (wrapperClass.isAssignableFrom(
        Reflection.getWrapperType(m.getReturnType())))
      {
        return (T) m.invoke(null, s);
      }

      ex = new ClassCastException(
        "Cannot assign the return type of " + m + " to " +
          targetClass.getName());
    }
    catch (InvocationTargetException ex1)
    {
      ex = ex1.getCause();
    }
    catch (ReflectiveOperationException | IllegalArgumentException ex1)
    {
      ex = ex1;
    }
    throw new IllegalArgumentException(
      "Cannot parse to " + targetClass.getName(), ex);
  }


  @SuppressWarnings("unchecked")
  public static <T> T parse( String s, T defaultValue )
    throws IllegalArgumentException
  {
    return parse(s, defaultValue, (Class<T>) defaultValue.getClass());
  }


  @SuppressWarnings("unchecked")
  public static <T> T parse( String s, T defaultValue, Class<T> clazz )
    throws IllegalArgumentException
  {
    return
      (s == null) ? defaultValue :
      (clazz.isInstance(s)) ? (T) s :
      valueOf(s, clazz);
  }


  private static final Map<String, Boolean> BOOLEAN_WORDS =
    new HashMap<String, Boolean>(12) {{
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
