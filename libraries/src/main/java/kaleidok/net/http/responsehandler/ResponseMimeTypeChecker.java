package kaleidok.net.http.responsehandler;

import kaleidok.net.http.util.MimeTypeMap;
import kaleidok.net.http.util.Parsers;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;

import java.io.IOException;


public class ResponseMimeTypeChecker
  implements ResponseHandler<HttpResponse>
{
  protected final MimeTypeMap acceptedMimeTypes;


  public ResponseMimeTypeChecker( MimeTypeMap acceptedMimeTypes )
  {
    this.acceptedMimeTypes = acceptedMimeTypes;
  }


  @Override
  public HttpResponse handleResponse( HttpResponse httpResponse )
    throws IOException
  {
    if (acceptedMimeTypes != null && !acceptedMimeTypes.isEmpty())
    {
      ContentType ct = Parsers.getContentType(httpResponse);
      String mimeType = (ct != null) ? ct.getMimeType() : null;
      if (acceptedMimeTypes.allows(mimeType) == null)
      {
        throw new ClientProtocolException(
          "Unsupported response MIME type: " + mimeType);
      }
    }
    return httpResponse;
  }
}
