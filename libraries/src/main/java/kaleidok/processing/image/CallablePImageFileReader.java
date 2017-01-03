package kaleidok.processing.image;

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

    ImageInputStream iis = new FileImageInputStream(source);
    try
    {
      initFrom(iis, null, FilenameUtils.getExtension(source.getPath()));
      iis = null;
    }
    finally
    {
      if (iis != null)
        iis.close();
    }
  }
}
