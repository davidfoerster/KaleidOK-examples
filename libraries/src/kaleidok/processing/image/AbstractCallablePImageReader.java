package kaleidok.processing.image;

import java.io.IOException;


public abstract class AbstractCallablePImageReader<S> extends CallablePImageReaderBase
{
  protected final S source;


  protected AbstractCallablePImageReader( S source )
  {
    this.source = source;
  }


  protected abstract void initFromSource() throws IOException;


  @Override
  protected void prepare() throws IOException
  {
    initFromSource();
  }


  @Override
  public S getSource()
  {
    return source;
  }
}
