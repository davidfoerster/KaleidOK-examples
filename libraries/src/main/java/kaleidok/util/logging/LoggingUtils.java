package kaleidok.util.logging;

import kaleidok.io.AnnotatedInputStream;
import kaleidok.util.Reflection;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public final class LoggingUtils
{
  private LoggingUtils() { }


  public static void logThrown( Logger logger, Level level, String msg,
    Throwable ex, Object[] params )
  {
    if (logger.isLoggable(level)) {
      LogRecord lr = new LogRecord(level, msg);
      lr.setParameters(params);
      lr.setThrown(ex);
      logger.log(lr);
    }
  }


  public static void logThrown( Logger logger, Level level, String msg,
    Throwable ex, Object param1 )
  {
    if (logger.isLoggable(level)) {
      LogRecord lr = new LogRecord(level, msg);
      lr.setParameters(new Object[]{param1});
      lr.setThrown(ex);
      logger.log(lr);
    }
  }


  public static boolean hasSystemLoggerConfiguration()
  {
    return !StringUtils.isEmpty(
      System.getProperty("java.util.logging.config.file"));
  }


  @SuppressWarnings("UnusedReturnValue")
  public static boolean loadLocalLoggerProperties( Class<?> contextClass )
  {
    String loggingFile = "logging.properties";
    AnnotatedInputStream is;
    try
    {
      is = newInputStream(loggingFile, contextClass.getClassLoader());
      try
      {
        LogManager.getLogManager().readConfiguration(is);
      }
      finally
      {
        is.close();
      }
    }
    catch (IOException ex)
    {
      logThrown(Logger.getAnonymousLogger(), Level.SEVERE,
        "Couldn’t load default {0} file for {1}", ex,
        new Object[]{loggingFile, contextClass.getName()});
      return false;
    }

    Logger.getLogger(contextClass.getCanonicalName()).log(Level.CONFIG,
      "Loaded logger configuration from: {0}", is.annotation);

    return true;
  }


  private static AnnotatedInputStream newInputStream( String filename,
    ClassLoader cl )
    throws FileNotFoundException
  {
    FileNotFoundException thrown = null;

    File file = new File(filename);
    if (file.exists()) try
    {
      return new AnnotatedInputStream(
        new FileInputStream(file), file.getAbsolutePath());
    }
    catch (FileNotFoundException ex)
    {
      thrown = ex;
    }

    if (cl != null)
    {
      URL resource = cl.getResource(filename);
      if (resource != null) try
      {
        return new AnnotatedInputStream(
          resource.openStream(), resource.toExternalForm());
      }
      catch (IOException ex)
      {
        throw new AssertionError(ex);
      }
    }

    throw (thrown != null) ? thrown : new FileNotFoundException(filename);
  }


  public static Logger getLogger( Class<?> clazz )
  {
    return
      (clazz.isPrimitive() || clazz.isArray() || clazz == void.class) ?
        Logger.getAnonymousLogger() :
        Logger.getLogger(Reflection.getTopLevelClass(clazz).getName());
  }


  public static void logAssertion( Class<?> assertionClass, Logger logger,
    Level level, String msg, Throwable ex, Object... params )
  {
    logThrown(logger, level, msg, ex, params);

    if (assertionClass.desiredAssertionStatus())
    {
      throw new AssertionError(MessageFormat.format(msg, params), ex);
    }
  }


  public static void logAssertion( Logger logger, Level level, String msg,
    Throwable thrown, Object... params )
  {
    logAssertion(sun.reflect.Reflection.getCallerClass(), logger, level, msg,
      thrown, params);
  }
}
