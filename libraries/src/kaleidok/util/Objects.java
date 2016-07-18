package kaleidok.util;

import java.lang.reflect.InvocationTargetException;

import static kaleidok.util.Arrays.EMPTY_CLASSES;
import static kaleidok.util.Arrays.EMPTY_OBJECTS;


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
  public static <T> T clone( T o )
    throws CloneNotSupportedException
  {
    Class<?> clazz = o.getClass();
    Object clone;
    try
    {
      clone = clazz.getMethod("clone", EMPTY_CLASSES).invoke(o, EMPTY_OBJECTS);
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
        "#clone() should only ever throw " +
          CloneNotSupportedException.class.getCanonicalName(),
        cause);
    }
    catch (NoSuchMethodException ignored)
    {
      return null;
    }
    if (clazz.isInstance(clone))
    {
      //noinspection unchecked
      return (T) clone;
    }
    throw new InternalError(
      clazz.getCanonicalName() + ("#clone() violated its contract and " +
        "returned a different type"),
      (clone != null) ?
        new ClassCastException(
          "Cannot cast " + clone.getClass().getName() + " to " +
            clazz.getName()) :
        new NullPointerException());
  }
}
