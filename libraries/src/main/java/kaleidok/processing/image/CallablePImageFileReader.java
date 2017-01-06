package kaleidok.processing.image;

import kaleidok.io.IOResourceWrapper;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;


public class CallablePImageFileReader
  extends AbstractCallablePImageReader<File>
{
  protected CallablePImageFileReader( File source )
  {
    super(source);
  }


  @Override
  protected void initFromSource()
    throws IOException, IllegalArgumentException
  {
    checkInitializable();

    String extension = FilenameUtils.getExtension(source.getPath());
    try (IOResourceWrapper<ImageInputStream> iis =
      new IOResourceWrapper<>(new FileImageInputStream(source)))
    {
      initFrom(iis.get(), null, extension);
      iis.release();
    }
  }
}
