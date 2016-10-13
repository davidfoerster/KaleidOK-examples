package kaleidok.util.logging;

import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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


  public static boolean loadLocalLoggerProperties( Class<?> contextClass )
  {
    String loggingFile = "logging.properties";
    try (InputStream is =
      newInputStream(loggingFile, contextClass.getClassLoader()))
    {
      LogManager.getLogManager().readConfiguration(is);
    }
    catch (IOException ex)
    {
      logThrown(Logger.getAnonymousLogger(), Level.SEVERE,
        "Couldn't load default {0} file for {1}", ex,
        new Object[]{loggingFile, contextClass.getName()});
      return false;
    }
    return true;
  }


  private static InputStream newInputStream( String filename,
    ClassLoader cl )
    throws FileNotFoundException
  {
    try
    {
      return new FileInputStream(filename);
    }
    catch (FileNotFoundException ex)
    {
      if (cl != null)
      {
        InputStream is = cl.getResourceAsStream(filename);
        if (is != null)
          return is;
      }
      throw ex;
    }
  }
}
