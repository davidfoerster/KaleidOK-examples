package kaleidok.util;


public final class AssertionUtils
{
  private AssertionUtils() { }


  public static void fastAssert( boolean assertionValue )
  {
    fastAssert(assertionValue, null);
  }

  public static void fastAssert( boolean assertionValue, String message )
  {
    if (!assertionValue)
      throw new AssertionError(message);
  }

  public static void fastAssertFmt( boolean assertionValue, String format,
    Object... formatArgs )
  {
    if (!assertionValue)
      throw new AssertionError(String.format(format, formatArgs));
  }
}
