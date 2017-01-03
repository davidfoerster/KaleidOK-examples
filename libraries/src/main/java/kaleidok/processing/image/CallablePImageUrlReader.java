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
  protected void initFromSource() throws IOException, IllegalStateException
  {
    checkInitializable();

    URLConnection con = source.openConnection();
    con.setDoInput(true);
    con.setDoOutput(false);
    HttpURLConnection http =
      (con instanceof HttpURLConnection) ? (HttpURLConnection) con : null;
    if (http != null)
      http.setRequestProperty("Accept", IMAGE_MIMETYPE_MAP.toString());
    con.connect();

    try (InputStream is = con.getInputStream())
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

      try (InputStream decodedIs =
        Parsers.DECODERS.getDecodedStream(contentEncoding, is))
      {
        try (ImageInputStream iis =
          ImageIO.createImageInputStream(decodedIs))
        {
          initFrom(iis, mimeType, fileExtension);
        }
      }
    }
  }
}
