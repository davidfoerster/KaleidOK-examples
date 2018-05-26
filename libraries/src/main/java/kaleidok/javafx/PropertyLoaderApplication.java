package kaleidok.javafx;

import javafx.application.Application;
import kaleidok.io.AnnotatedInputStream;
import kaleidok.util.Reflection;
import kaleidok.util.containers.LowercaseStringMap;
import kaleidok.util.prefs.DefaultValueParser;
import kaleidok.util.logging.LoggingUtils;
import kaleidok.util.prefs.PropertyLoader;
import kaleidok.util.Strings;
import kaleidok.util.containers.ChainedMap;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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
      namedParameters =
        new ChainedMap<>(namedParameters, PropertyLoader.toMap(prop));
    }
    namedParameters =
      new LowercaseStringMap<>(namedParameters, Locale.ENGLISH);
  }


  protected Properties loadProperties() throws IOException
  {
    Class<?> clazz = Reflection.getNamedSuperClass(getClass());
    String defaultPropPath = clazz.getSimpleName() + ".properties";
    Properties prop = new Properties();
    List<Object> propFiles = new ArrayList<>(Arrays.asList(
      PropertyLoader.load(prop, null, clazz, defaultPropPath)));

    String requestedPropPath = getParameters().getNamed().get("params");
    if (requestedPropPath != null)
    {
      AnnotatedInputStream is = openFilenameArgument(requestedPropPath);
      try (Reader r = new InputStreamReader(is))
      {
        prop.load(r);
      }
      finally
      {
        is.close();
      }

      propFiles.add(is.annotation);
    }

    Logger logger = Logger.getLogger(clazz.getCanonicalName());
    if (!propFiles.isEmpty())
    {
      if (logger.isLoggable(Level.CONFIG))
      {
        logger.config(
          propFiles.stream().sequential().map(String::valueOf).collect(
            Collectors.joining(", ",
              "Loaded application configuration from: ", "")));
      }
    }
    else
    {
      logger.log(Level.CONFIG,
        "No Applet properties file \"{0}\" found; using default values",
        defaultPropPath);
    }

    return prop;
  }


  private static AnnotatedInputStream openFilenameArgument( String arg )
    throws FileNotFoundException
  {
    if (arg.length() == 1 && arg.charAt(0) == '-')
    {
      return new AnnotatedInputStream(System.in, "<stdin>");
    }

    File f = new File(arg);
    return new AnnotatedInputStream(new FileInputStream(f), f.getAbsolutePath());
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
