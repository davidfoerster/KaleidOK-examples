package kaleidok.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;


public final class Objects
{
  private Objects() { }


  /**
   * Reflectively calls the object’s {@code clone()} method.
   *
   * @param o  An object to clone
   * @return  A clone of {@code o} or {@code null} if {@code o.clone()} isn’t
   *    public.
   * @throws CloneNotSupportedException  as thrown by {@code o.clone()}
   * @see Object#clone()
   */
  public static <T extends Cloneable> T clone( T o )
    throws CloneNotSupportedException
  {
    Method cloneMethod;
    try
    {
      cloneMethod = o.getClass().getMethod("clone", EMPTY_CLASS_ARRAY);
    }
    catch (NoSuchMethodException ignored)
    {
      return null;
    }
    try
    {
      //noinspection unchecked
      return (T) cloneMethod.invoke(o, EMPTY_OBJECT_ARRAY);
    }
    catch (IllegalAccessException ex)
    {
      // Shouldn't happen since Class#getMethod(...) only returns public methods
      throw new AssertionError(ex);
    }
    catch (InvocationTargetException ex)
    {
      Throwable cause = ex.getCause();
      if (cause instanceof CloneNotSupportedException)
        throw (CloneNotSupportedException) cause;
      throw new InternalError(
        cloneMethod + " should only ever throw " +
          CloneNotSupportedException.class.getCanonicalName(),
        cause);
    }
  }


  public static int hashCode( int seed, int thing )
  {
    return 31 * seed + thing;
  }

  public static int hashCode( int seed, long thing )
  {
    return hashCode(seed, Long.hashCode(thing));
  }

  public static int hashCode( int seed, Object thing )
  {
    return hashCode(seed, java.util.Objects.hashCode(thing));
  }

  public static int hashCode( int seed, boolean thing )
  {
    return hashCode(seed, Boolean.hashCode(thing));
  }

  public static int hashCode( int seed, float thing )
  {
    return hashCode(seed, Float.hashCode(thing));
  }

  public static int hashCode( int seed, double thing )
  {
    return hashCode(seed, Double.hashCode(thing));
  }


  public static <T> T requireNonNull( T obj, boolean allowNull )
  {
    return requireNonNull(obj, allowNull, null);
  }

  public static <T> T requireNonNull( T obj, boolean allowNull, String message )
  {
    if (obj != null || allowNull)
      return obj;

    throw new NullPointerException(message);
  }


  public static String objectToString( Object obj )
  {
    if (obj == null)
      return String.valueOf((Object) null);

    String className = obj.getClass().getName();
    int classNameLength = className.length();
    char[] s = new char[classNameLength + (Integer.BYTES * 2 + 1)];
    className.getChars(0, classNameLength, s, 0);
    s[classNameLength] = '@';
    return new String(Strings.toHexDigits(
      System.identityHashCode(obj), s, classNameLength + 1, Integer.BYTES * 2));
  }
}
