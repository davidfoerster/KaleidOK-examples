package kaleidok.processing.image;

import kaleidok.http.util.Parsers;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.entity.ContentType;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static kaleidok.http.responsehandler.ImageMimeTypeChecker.IMAGE_MIMETYPE_MAP;


public class CallablePImageUrlReader extends AbstractCallablePImageReader<URL>
{
  protected CallablePImageUrlReader( URL source )
  {
    super(source);
  }


  @Override
  protected void initFromSource() throws IOException
  {
    URLConnection con = source.openConnection();
    con.setDoInput(true);
    con.setDoOutput(false);
    HttpURLConnection http =
      (con instanceof HttpURLConnection) ? (HttpURLConnection) con : null;
    if (http != null)
      http.setRequestProperty("Accept", IMAGE_MIMETYPE_MAP.toString());
    con.connect();

    InputStream is = null;
    ImageInputStream iis = null;
    try
    {
      String fileExtension =
        FilenameUtils.getExtension(con.getURL().getPath());
      String contentType = con.getContentType();
      String mimeType = (contentType != null) ?
        ContentType.parse(contentType).getMimeType() :
        null;

      String contentEncoding = con.getContentEncoding();
      if (contentEncoding == null) {
        contentEncoding = con.getHeaderField("transfer-encoding");
        if ("chunked".equals(contentEncoding))
          contentEncoding = null;
      }

      is =
        Parsers.DECODERS.getDecodedStream(contentEncoding,
          con.getInputStream());
      iis = ImageIO.createImageInputStream(is);
      is = null;

      initFrom(iis, mimeType, fileExtension);
      iis = null;
    }
    finally
    {
      if (iis != null)
        iis.close();
      if (is != null)
        is.close();
    }
  }


  @Override
  protected void dispose()
  {
    super.dispose();
  }
}
