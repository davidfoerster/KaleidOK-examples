package kaleidok.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;


public final class Readers
{
  private Readers() { }

  public static final int BUFFERSIZE_MIN = 64 << 10;

  public static String readAll( final Reader r, char[] charBuf )
    throws IOException
  {
    if (charBuf == null || charBuf.length == 0)
      charBuf = new char[BUFFERSIZE_MIN];

    int current, total = 0;
    while ((current = r.read(charBuf, total, charBuf.length - total)) >= 0)
    {
      total += current;

      if (total > charBuf.length - BUFFERSIZE_MIN) {
        int newSize;
        if (charBuf.length <= Integer.MAX_VALUE / 2) {
          newSize = Math.max(charBuf.length * 2, BUFFERSIZE_MIN);
        } else if (total != Integer.MAX_VALUE) {
          if (charBuf.length == Integer.MAX_VALUE)
            continue;
          newSize = Integer.MAX_VALUE;
        } else {
          throw new IOException("Maximum buffer size exceeded");
        }
        charBuf = Arrays.copyOf(charBuf, newSize);
      }
    }

    return (total != 0) ? new String(charBuf, 0, total) : "";
  }

}
