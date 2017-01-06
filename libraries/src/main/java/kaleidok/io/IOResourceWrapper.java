package kaleidok.io;

import kaleidok.util.AbstractResourceWrapper;

import java.io.Closeable;
import java.io.IOException;


public class IOResourceWrapper<T extends Closeable>
  extends AbstractResourceWrapper<T> implements Closeable
{
  public IOResourceWrapper( T resource )
  {
    super(resource);
  }


  @SuppressWarnings("resource")
  @Override
  public void close() throws IOException
  {
    T resource = get();
    if (resource != null)
      resource.close();
  }
}
