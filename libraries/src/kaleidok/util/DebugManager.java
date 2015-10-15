package kaleidok.util;

import java.applet.Applet;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class DebugManager
{
  public static int verbose = 0;

  public static int wireframe = 0;

  public static int debug = 0;
  static {
    String prefix = "-agentlib:jdwp";
    for (String arg:
      ManagementFactory.getRuntimeMXBean().getInputArguments())
    {
      if (arg.startsWith(prefix) &&
        (arg.length() == prefix.length() || arg.charAt(prefix.length()) == '='))
      {
        debug = 1;
        break;
      }
    }
  }


  private static final Logger logger =
    Logger.getLogger(DebugManager.class.getCanonicalName());

  private static Object source = null;

  private static void checkAlreadyInitialized( Object source )
  {
    if (DebugManager.source != null) {
      if (source != DebugManager.source) {
        throw new IllegalStateException(
          DebugManager.class.getCanonicalName() +
            " was already initialized earlier from the different source " +
            DebugManager.source);
      } else if (verbose >= 1) {
        logger.log(Level.WARNING,
          "Re-initializing from the same object \"{0}\" as earlier", source);
      }
    }
  }


  private DebugManager() { }

  public static void fromSystem()
  {
    checkAlreadyInitialized(System.class);
    fromPropertyGetter(new PropertyGetter()
    {
      @Override
      public String get( String name )
      {
        return System.getProperty(name);
      }
    });
  }

  public static void fromApplet( final Applet a )
  {
    checkAlreadyInitialized(a);
    fromPropertyGetter(new PropertyGetter()
    {
      @Override
      public String get( String name )
      {
        return a.getParameter(name);
      }
    });
  }


  private static void fromPropertyGetter( PropertyGetter pg )
  {
    final String className = DebugManager.class.getCanonicalName();

    for (Field f: DebugManager.class.getDeclaredFields())
    {
      int mod = f.getModifiers();
      if (Modifier.isPublic(mod) && Modifier.isStatic(mod))
      {
        String strValue = pg.get(className + '.' + f.getName());
        if (strValue != null) {
          Class<?> clazz = DefaultValueParser.getWrapperType(f.getType());
          try {
            f.set(null, DefaultValueParser.valueOf(strValue, clazz));
          } catch (IllegalAccessException ex) {
            // we already checked for that
            throw new AssertionError(ex);
          }
        }
      }
    }
  }


  /*
  public static void printThreads( PrintStream out, ThreadGroup tg, boolean recurse )
  {
    if (out == null)
      out = System.out;
    if (tg == null)
      tg = AppContext.getAppContext().getThreadGroup();

    Thread[] threads = new Thread[1 << 4];
    int count;
    while ((count = tg.enumerate(threads, recurse)) >= threads.length)
      threads = new Thread[java.lang.Math.max(threads.length << 1, count + 1)];

    out.format(
      "Thread group \"%s\" (%d): %d children, daemon=%s, maxPriority=%d:%n",
      tg.getName(), System.identityHashCode(tg), count, tg.isDaemon(),
      tg.getMaxPriority());
    for (int i = 0; i < count; i++) {
      final Thread t = threads[i];
      out.format(
        "Thread \"%s\" (%d): state=%s, daemon=%s, priority=%d, group=\"%s\" (%d)%n",
        t.getName(), t.getId(), t.getState().name(), t.isDaemon(),
        t.getPriority(), t.getThreadGroup().getName(),
        System.identityHashCode(t.getThreadGroup()));
    }
    if (count > 0)
      out.println();
  }
  */


  private interface PropertyGetter
  {
    String get( String name );
  }
}
