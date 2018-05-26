package kaleidok.io;

import java.io.IOException;
import java.io.InputStream;


public class AnnotatedInputStream extends InputStream
{
  private final InputStream underlying;

  public final Object annotation;


  public AnnotatedInputStream( InputStream underlying, Object annotation )
  {
    this.underlying = underlying;
    this.annotation = annotation;
  }


  @Override
  public int read() throws IOException
  {
    return underlying.read();
  }

  @Override
  public int read( byte[] b ) throws IOException
  {
    return underlying.read(b);
  }

  @Override
  public int read( byte[] b, int off, int len ) throws IOException
  {
    return underlying.read(b, off, len);
  }

  @Override
  public long skip( long n ) throws IOException
  {
    return underlying.skip(n);
  }

  @Override
  public int available() throws IOException
  {
    return underlying.available();
  }

  @Override
  public void close() throws IOException
  {
    underlying.close();
  }

  @Override
  public void mark( int readlimit )
  {
    underlying.mark(readlimit);
  }

  @Override
  public void reset() throws IOException
  {
    underlying.reset();
  }

  @Override
  public boolean markSupported()
  {
    return underlying.markSupported();
  }
}
