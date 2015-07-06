package kaleidok.http.responsehandler;

import kaleidok.http.util.MimeTypeMap;
import kaleidok.http.util.Parsers;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

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
    Parsers.ContentType ct = Parsers.getContentType(httpResponse);
    if (acceptedMimeTypes != null && !acceptedMimeTypes.isEmpty()) {
      String mimeType = (ct != null) ? ct.mimeType : null;
      if (acceptedMimeTypes.allows(mimeType) == null)
        throw new ClientProtocolException("Unsupported response MIME type: " + mimeType);
    }
    return httpResponse;
  }
}
