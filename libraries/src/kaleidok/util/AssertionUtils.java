package kaleidok.util;

import java.lang.management.ManagementFactory;


public final class AssertionUtils
{
  private AssertionUtils() { }


  public static void enableAssertionsOnDebugging()
  {
    String prefix = "-agentlib:jdwp";
    for (String arg:
      ManagementFactory.getRuntimeMXBean().getInputArguments())
    {
      if (arg.startsWith(prefix) &&
        (arg.length() == prefix.length() || arg.charAt(prefix.length()) == '='))
      {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
        break;
      }
    }
  }
}
