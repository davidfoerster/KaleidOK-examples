package kaleidok.util.prefs;


import kaleidok.util.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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


  public static <T> T valueOf( String s, Class<T> targetClass )
    throws IllegalArgumentException
  {
    Class<?> wrapperClass = Reflection.getWrapperType(targetClass);
    Throwable ex;
    if (wrapperClass == Character.class)
    {
      if (s.length() == 1)
      {
        //noinspection unchecked
        return (T) Character.valueOf(s.charAt(0));
      }

      ex = new IllegalArgumentException(String.format(
        "Cannot cast string of length %d to %s.",
        s.length(), targetClass.getName()));
    }
    else try
    {
      Method m = wrapperClass.getMethod("valueOf", valueOfParameterTypes);
      if (!Modifier.isStatic(m.getModifiers()))
      {
        ex = new NoSuchMethodException(m + " is an instance method.");
      }
      else if (!wrapperClass.isAssignableFrom(
        Reflection.getWrapperType(m.getReturnType())))
      {
        ex = new ClassCastException(String.format(
          "Cannot cast the return type of %s to %s.",
          m, targetClass.getName()));
      }
      else
      {
        //noinspection unchecked
        return (T) m.invoke(null, s);
      }
    }
    catch (NoSuchMethodException ex1)
    {
      ex = ex1;
    }
    catch (InvocationTargetException ex1)
    {
      ex = ex1.getCause();
    }
    catch (IllegalAccessException | IllegalArgumentException ex1)
    {
      /*
       * This shouldn't happen; Class#getMethod() returns only public methods,
       * we verified that it's static and has the right argument type.
       */
      throw new InternalError(ex1);
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


  private static final Map<String, Boolean> BOOLEAN_WORDS;

  static
  {
    Map<String, Boolean> m = BOOLEAN_WORDS = new HashMap<>(12);
    m.put("true", TRUE);
    m.put("false", FALSE);
    m.put("yes", TRUE);
    m.put("no", FALSE);
    m.put("on", TRUE);
    m.put("off", FALSE);
    m.put("enabled", TRUE);
    m.put("disabled", FALSE);
  }
}
