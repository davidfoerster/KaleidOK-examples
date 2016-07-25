package kaleidok.util;

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
    String systemLoggerConfigPath =
      System.getProperty("java.util.logging.config.file");
    return systemLoggerConfigPath != null && !systemLoggerConfigPath.isEmpty();
  }


  @SuppressWarnings({ "resource", "IOResourceOpenedButNotSafelyClosed" })
  public static void loadLocalLoggerProperties( Class<?> contextClass )
  {
    String loggingFile = "logging.properties";
    InputStream is;
    try
    {
      is = new FileInputStream(loggingFile);
    }
    catch (FileNotFoundException ignored)
    {
      is = contextClass.getClassLoader().getResourceAsStream(loggingFile);
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
        new Object[]{loggingFile, contextClass.getName()});
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
