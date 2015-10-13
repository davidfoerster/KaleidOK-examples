package kaleidok.util;

import java.util.logging.Level;
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
}
