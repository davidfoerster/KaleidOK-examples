package kaleidok.util;

import java.lang.management.ManagementFactory;


public final class AssertionUtils
{
  private AssertionUtils() { }


  public static boolean enableAssertionsOnDebugging()
  {
    for (String arg:
      ManagementFactory.getRuntimeMXBean().getInputArguments())
    {
      //noinspection SpellCheckingInspection
      if (Strings.startsWithToken(arg, "-agentlib:jdwp", '='))
      {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
        return true;
      }
    }
    return false;
  }


  public static void enablePackageAssertions( Class<?> clazz )
  {
    clazz.getClassLoader()
      .setPackageAssertionStatus(clazz.getPackage().getName(), true);
  }
}
