package kaleidok.processing.image;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;


public class IIOReadProgressAbortListener
  implements IIOReadProgressListener
{
  public static final IIOReadProgressAbortListener INSTANCE =
    new IIOReadProgressAbortListener();

  protected IIOReadProgressAbortListener() { }

  @Override
  public void sequenceStarted( ImageReader source, int minIndex )
  {
    source.abort();
  }

  @Override
  public void sequenceComplete( ImageReader source ) { }

  @Override
  public void imageStarted( ImageReader source, int imageIndex )
  {
    source.abort();
  }

  @Override
  public void imageProgress( ImageReader source, float percentageDone )
  {
    source.abort();
  }

  @Override
  public void imageComplete( ImageReader source ) { }

  @Override
  public void thumbnailStarted( ImageReader source, int imageIndex, int thumbnailIndex )
  {
    source.abort();
  }

  @Override
  public void thumbnailProgress( ImageReader source, float percentageDone )
  {
    source.abort();
  }

  @Override
  public void thumbnailComplete( ImageReader source ) { }

  @Override
  public void readAborted( ImageReader source )
  {
  }
}
