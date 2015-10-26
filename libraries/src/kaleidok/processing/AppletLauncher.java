package kaleidok.processing;

import kaleidok.util.PropertyLoader;
import sun.applet.AppletViewer;
import sun.applet.AppletViewerFactory;

import java.applet.Applet;
import java.awt.Rectangle;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
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
      int i;
      for (i = 0; i < args.length; i++) {
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
      }
    }

    buildAttributes(attributes, appletClass, null);
    return launch0(appletClass, attributes, 0, 0);
  }


  public AppletViewer launch( Class<? extends Applet> appletClass,
    String... args ) throws IOException
  {
    Properties prop = new Properties();
    {
      String propPath = appletClass.getSimpleName() + ".properties";
      if (PropertyLoader.load(prop, null, appletClass, propPath) == 0) {
        Logger.getLogger(appletClass.getCanonicalName()).log(Level.CONFIG,
          "No Applet properties file \"{0}\" found; using default values",
          propPath);
      }
    }

    if (args != null && args.length > 0 && args[0].equals("--params")) {
      String paramsFile = args[1];
      prop.load(new InputStreamReader(
        (paramsFile.length() == 1 && paramsFile.charAt(0) == '-') ?
          System.in :
          new FileInputStream(paramsFile)));

      args = (args.length > 2) ? Arrays.copyOfRange(args, 2, args.length) : null;
    }

    return launch(appletClass, prop, args);
  }


  public void loadLocalLoggerProperties( Class<?> appletClass )
  {
    File loggingFile = new File("logging.properties");
    InputStream is = null;
    try
    {
      is = loggingFile.exists() ?
        new FileInputStream(loggingFile) :
        appletClass.getClassLoader().getResourceAsStream(loggingFile.getPath());
      if (is != null)
        LogManager.getLogManager().readConfiguration(is);
    }
    catch (IOException ex)
    {
      logThrown(Logger.getAnonymousLogger(), Level.SEVERE,
        "Couldn't load default {0} file for {1}", ex,
        new Object[]{loggingFile.getPath(), appletClass.getName()});
    }
    finally
    {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          Logger.getAnonymousLogger().log(Level.WARNING,
            "Couldn't close logger configuration file", ex);
        }
      }
    }
  }
}
