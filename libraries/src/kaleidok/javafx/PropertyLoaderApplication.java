package kaleidok.javafx;

import javafx.application.Application;
import kaleidok.util.containers.LowercaseStringMap;
import kaleidok.util.prefs.DefaultValueParser;
import kaleidok.util.logging.LoggingUtils;
import kaleidok.util.prefs.PropertyLoader;
import kaleidok.util.Strings;
import kaleidok.util.containers.ChainedMap;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class PropertyLoaderApplication extends Application
{
  private Map<String, String> namedParameters;

  private List<String> unnamedParameters;


  public Map<String, String> getNamedParameters()
  {
    return namedParameters;
  }


  public List<String> getUnnamedParameters()
  {
    return unnamedParameters;
  }


  public boolean getUnnamedBooleanParameter( final String name )
  {
    String sVal = getNamedParameters().get(name);
    return (sVal != null) ?
      DefaultValueParser.parseBoolean(sVal) :
      getUnnamedParameters().stream().anyMatch(
        (s) -> Strings.isConcatenation(s, "--", name));
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void init() throws Exception
  {
    Parameters params = getParameters();
    namedParameters = params.getNamed();
    unnamedParameters = params.getUnnamed();

    Properties prop = loadProperties();
    if (!prop.isEmpty())
    {
      namedParameters = Collections.unmodifiableMap(
        new ChainedMap<>(namedParameters, PropertyLoader.toMap(prop, null)));
    }
    namedParameters =
      new LowercaseStringMap<>(namedParameters, Locale.ENGLISH);
  }


  protected Properties loadProperties() throws IOException
  {
    Properties prop = new Properties();
    String defaultPropPath = getClass().getSimpleName() + ".properties";
    boolean hasDefaultProperties =
      PropertyLoader.load(prop, null, getClass(), defaultPropPath) > 0;

    String requestedPropPath = getParameters().getNamed().get("params");
    if (requestedPropPath != null)
    {
      try (Reader r =
        new InputStreamReader(openFilenameArgument(requestedPropPath)))
      {
        prop.load(r);
      }
    }
    else if (!hasDefaultProperties)
    {
      Logger.getLogger(getClass().getCanonicalName()).log(Level.CONFIG,
        "No Applet properties file \"{0}\" found; using default values",
        defaultPropPath);
    }

    return prop;
  }


  private static InputStream openFilenameArgument( String arg )
    throws FileNotFoundException
  {
    return (arg.length() == 1 && arg.charAt(0) == '-') ?
      System.in :
      new FileInputStream(arg);
  }


  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static void launch( Class<? extends Application> appClass,
    String... args )
  {
    if (!LoggingUtils.hasSystemLoggerConfiguration())
      LoggingUtils.loadLocalLoggerProperties(appClass);
    Application.launch(appClass, args);
  }
}
