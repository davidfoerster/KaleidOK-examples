package kaleidok.http.responsehandler;

import kaleidok.http.util.Parsers;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class ReaderResponseHandler
  implements ResponseHandler<Reader>
{
  public static final ReaderResponseHandler UTF8_HANDLER =
    new ReaderResponseHandler(StandardCharsets.UTF_8);


  public final Charset defaultCharset;


  public ReaderResponseHandler( Charset defaultCharset )
  {
    this.defaultCharset = defaultCharset;
  }


  @Override
  public Reader handleResponse( HttpResponse httpResponse )
    throws IOException
  {
    ContentType ct = Parsers.getContentType(httpResponse);
    Charset charset = (ct != null) ? ct.getCharset() : null;
    if (charset == null) {
      charset = defaultCharset;
      if (charset == null)
        throw new ClientProtocolException("Server returned invalid charset");
    }
    return new InputStreamReader(httpResponse.getEntity().getContent(), charset);
  }
}
