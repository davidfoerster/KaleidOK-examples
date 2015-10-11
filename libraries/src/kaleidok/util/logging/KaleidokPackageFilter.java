package kaleidok.util.logging;

import com.getflourish.stt2.STT;

import java.util.Arrays;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;


public class KaleidokPackageFilter implements Filter
{
  @Override
  public boolean isLoggable( LogRecord record )
  {
    return record.getLevel().intValue() >= Level.INFO.intValue() ||
      isLoggable(record.getLoggerName());
  }


  private boolean isLoggable( String loggerName )
  {
    int p = Arrays.binarySearch(localResources, loggerName);
    if (p >= 0)
      return true;
    p = -2 - p;
    if (p < 0)
      return false;
    String match = localResources[p];
    return loggerName.startsWith(match) &&
      (loggerName.length() == match.length() ||
         loggerName.charAt(match.length()) == '.');
  }


  private static final String[] localResources = {
      "kaleidok",
      STT.class.getPackage().getName()
    };
  static {
    Arrays.sort(localResources);
  }
}
