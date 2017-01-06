package kaleidok.http.responsehandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.io.Reader;


class ReaderSource
{
  public final ResponseHandler<Reader> readerSource;


  public ReaderSource( ResponseHandler<Reader> readerSource )
  {
    this.readerSource = readerSource;
  }


  protected Reader getReader( HttpResponse httpResponse ) throws IOException
  {
    return readerSource.handleResponse(httpResponse);
  }
}
