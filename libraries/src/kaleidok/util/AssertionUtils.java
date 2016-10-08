package kaleidok.util;

import java.lang.management.ManagementFactory;


public final class AssertionUtils
{
  private AssertionUtils() { }


  public static boolean enableAssertionsOnDebugging()
  {
    @SuppressWarnings("SpellCheckingInspection")
    boolean remoteDebuggingStatus =
      ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .anyMatch((arg) ->
          Strings.startsWithToken(arg, "-agentlib:jdwp", '='));
    if (remoteDebuggingStatus)
      ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    return remoteDebuggingStatus;
  }


  public static void enablePackageAssertions( Class<?> clazz )
  {
    clazz.getClassLoader()
      .setPackageAssertionStatus(clazz.getPackage().getName(), true);
  }


  public static void fastAssert( boolean assertionValue )
  {
    fastAssert(assertionValue, null);
  }

  public static void fastAssert( boolean assertionValue, String message )
  {
    if (!assertionValue)
      throw new AssertionError(message);
  }
}
