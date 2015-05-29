package kaleidok.util;

import java.applet.Applet;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


public final class DebugManager
{
  public static int verbose = 0;

  public static int wireframe = 0;

  public static int debug = 0;
  static {
    String prefix = "-agentlib:jdwp";
    for (String arg: ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (arg.startsWith(prefix) && (arg.length() == prefix.length() || arg.charAt(prefix.length()) == '=')) {
        debug = 1;
        break;
      }
    }
  }


  private DebugManager() { }

  public static void fromSystem()
  {
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
    final String base = DebugManager.class.getCanonicalName() + '.';
    final Class<?>[] paramTypes = new Class<?>[]{ String.class };
    final Object[] params = new Object[1];

    for (Field f: DebugManager.class.getDeclaredFields())
    {
      int mod = f.getModifiers();
      if (Modifier.isPublic(mod) && Modifier.isStatic(mod))
      {
        params[0] = pg.get(base + f.getName());
        if (params[0] != null)
        {
          Class<?> clazz = f.getType();
          if (clazz.isPrimitive())
            clazz = PRIMITIVES_TO_WRAPPERS.get(clazz);
          try {
            Object val = (clazz != String.class) ?
              clazz.getMethod("valueOf", paramTypes).invoke(null, params) :
              params[0];
            f.set(null, val);
          } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | IllegalArgumentException | NullPointerException ex) {
            throw new UnsupportedOperationException(
              clazz.getName() + " cannot be created with #valueOf(String)",
              ex);
          }
        }
      }
    }
  }

  private interface PropertyGetter
  {
    String get( String name );
  }


  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS =
    new HashMap<Class<?>, Class<?>>(16) {{
      put( boolean.class, Boolean.class );
      put( byte.class, Byte.class );
      put( char.class, Character.class );
      put( double.class, Double.class );
      put( float.class, Float.class );
      put( int.class, Integer.class );
      put( long.class, Long.class );
      put( short.class, Short.class );
    }};
}
