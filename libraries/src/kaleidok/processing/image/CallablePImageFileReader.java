package kaleidok.processing.image;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
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
    ImageReader imageReader =
      getFirstImageReader(ImageIO.getImageReadersBySuffix(
        FilenameUtils.getExtension(source.getPath())));
    ImageInputStream iis = null;
    try
    {
      iis = new FileImageInputStream(source);
      initFrom(iis, imageReader);
      iis = null;
      imageReader = null;
    }
    finally
    {
      if (iis != null)
        iis.close();
      if (imageReader != null)
        imageReader.dispose();
    }
  }
}
