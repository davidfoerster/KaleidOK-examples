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
    if (charBuf == null)
      charBuf = new char[BUFFERSIZE_MIN];

    int bytesRead = 0;
    while (true) {
      if (bytesRead < charBuf.length - BUFFERSIZE_MIN) {
        int newSize;
        if (charBuf.length <= Integer.MAX_VALUE / 2) {
          newSize = charBuf.length * 2;
        } else {
          if (bytesRead == Integer.MAX_VALUE)
            throw new IOException("Maximum buffer size exceeded");
          newSize = Integer.MAX_VALUE;
        }
        if (newSize > charBuf.length)
          charBuf = Arrays.copyOf(charBuf, newSize);
      }

      int count = r.read(charBuf, bytesRead, charBuf.length - bytesRead);
      if (count < 0)
        break;
      bytesRead += count;
    }

    return (bytesRead != 0) ? new String(charBuf, 0, bytesRead) : "";
  }

}
