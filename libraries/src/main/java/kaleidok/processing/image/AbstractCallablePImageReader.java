package kaleidok.processing.image;

import java.io.IOException;
import java.util.Objects;


public abstract class AbstractCallablePImageReader<S> extends CallablePImageReaderBase
{
  protected final S source;


  protected AbstractCallablePImageReader( S source )
  {
    this.source = Objects.requireNonNull(source);
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
