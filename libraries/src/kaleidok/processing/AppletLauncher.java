package kaleidok.processing;

import kaleidok.util.PropertyLoader;
import sun.applet.AppletViewer;
import sun.applet.AppletViewerFactory;

import java.applet.Applet;
import java.awt.Rectangle;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static kaleidok.util.LoggingUtils.logThrown;


public class AppletLauncher
{
  public final AppletViewerFactory appletViewerFactory;

  public AppletLauncher( AppletViewerFactory appletViewerFactory )
  {
    this.appletViewerFactory = appletViewerFactory;
  }


  public AppletViewer launch( Class<? extends Applet> appletClass,
    Rectangle bounds )
  {
    Hashtable<String, String> attributes = new Hashtable<>();
    buildAttributes(attributes, appletClass, bounds);
    return launch0(appletClass, attributes, bounds.x, bounds.y);
  }


  public AppletViewer launch( Class<? extends Applet> appletClass,
    Map<String, String> attributes )
  {
    Hashtable<String, String> _attributes = new Hashtable<>(attributes);
    buildAttributes(_attributes, appletClass, null);
    return launch0(appletClass, _attributes, 0, 0);
  }


  private AppletViewer launch0( Class<? extends Applet> appletClass,
    Hashtable<String, String> attributes, int x, int y )
  {
    String systemLoggerConfigPath =
      System.getProperty(Logger.class.getPackage().getName() + ".config.file");
    if (systemLoggerConfigPath == null || systemLoggerConfigPath.isEmpty())
      loadLocalLoggerProperties(appletClass);

    URL documentURL = appletClass.getResource(".");
    if (documentURL == null) {
      documentURL = appletClass.getProtectionDomain().getCodeSource().getLocation();
      if (documentURL == null)
        throw new Error("Cannot devise document URL");
    }
    return appletViewerFactory.createAppletViewer(x, y,
      documentURL, attributes);
  }


  private static void buildAttributes( Map<String, String> atts,
    Class<? extends Applet> appletClass, Rectangle bounds )
  {
    atts.put("codebase", ".");
    atts.put("code", appletClass.getName());

    if (!atts.containsKey("name"))
      atts.put("name", appletClass.getSimpleName());

    if (bounds != null) {
      atts.put("width", Integer.toString(bounds.width));
      atts.put("height", Integer.toString(bounds.height));
    } else {
      if (!atts.containsKey("width"))
        atts.put("width", "100");
      if (!atts.containsKey("height"))
        atts.put("height", "100");
    }
  }


  public AppletViewer launch( Class<? extends Applet> appletClass,
    Map<?, ?> properties, String... args )
  {
    Hashtable<String, String> attributes = new Hashtable<>();

    if (properties != null) {
      for (Map.Entry<?, ?> param : properties.entrySet())
        attributes.put(param.getKey().toString(), param.getValue().toString());
    }

    if (args != null) {
      int i = 0;
      while (i < args.length) {
        final String arg = args[i];
        switch (arg) {
        case "-p":
        case "--param":
          String param = args[++i];
          int p = param.indexOf('=');
          if (p <= 0) {
            throw new IllegalArgumentException(
              "Invalid parameter specification: " + param);
          }
          attributes.put(param.substring(0, p), param.substring(p + 1));
          break;

        case "--height":
        case "--width":
        case "--name":
          attributes.put(arg.substring(2), args[++i]);
          break;

        default:
          throw new IllegalArgumentException("Illegal parameter: " + arg);
        }
        i++;
      }
    }

    buildAttributes(attributes, appletClass, null);
    return launch0(appletClass, attributes, 0, 0);
  }


  public AppletViewer launch( Class<? extends Applet> appletClass,
    String... args ) throws IOException
  {
    Properties prop = new Properties();
    String propPath = appletClass.getSimpleName() + ".properties";
    boolean hasDefaultProperties =
      PropertyLoader.load(prop, null, appletClass, propPath) > 0;

    if (args != null && args.length > 0 && "--params".equals(args[0]))
    {
      if (args.length < 2)
      {
        throw new IllegalArgumentException(
          "Option \"--params\" requires an argument");
      }
      try (Reader r = new InputStreamReader(openFilenameArgument(args[1])))
      {
        prop.load(r);
      }
      args = (args.length > 2) ? Arrays.copyOfRange(args, 2, args.length) : null;
    }
    else if (!hasDefaultProperties)
    {
      Logger.getLogger(appletClass.getCanonicalName()).log(Level.CONFIG,
        "No Applet properties file \"{0}\" found; using default values",
        propPath);
    }

    return launch(appletClass, prop, args);
  }


  private static InputStream openFilenameArgument( String arg )
    throws FileNotFoundException
  {
    Objects.requireNonNull(arg);
    return (arg.length() == 1 && arg.charAt(0) == '-') ?
      System.in :
      new FileInputStream(arg);
  }


  @SuppressWarnings({ "resource", "IOResourceOpenedButNotSafelyClosed" })
  public static void loadLocalLoggerProperties( Class<?> appletClass )
  {
    String loggingFile = "logging.properties";
    InputStream is;
    try
    {
      is = new FileInputStream(loggingFile);
    }
    catch (FileNotFoundException ignored)
    {
      is = appletClass.getClassLoader().getResourceAsStream(loggingFile);
      if (is == null)
        return;
    }

    try
    {
      LogManager.getLogManager().readConfiguration(is);
    }
    catch (IOException ex)
    {
      logThrown(Logger.getAnonymousLogger(), Level.SEVERE,
        "Couldn't load default {0} file for {1}", ex,
        new Object[]{loggingFile, appletClass.getName()});
    }
    finally
    {
      try {
        is.close();
      } catch (IOException ex) {
        Logger.getAnonymousLogger().log(Level.WARNING,
          "Couldn't close logger configuration file", ex);
      }
    }
  }
}
