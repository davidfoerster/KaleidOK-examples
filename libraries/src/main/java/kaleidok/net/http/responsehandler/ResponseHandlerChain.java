package kaleidok.net.http.responsehandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;


public class ResponseHandlerChain<T>
  implements ResponseHandler<T>
{
  protected ResponseHandler<HttpResponse> first;

  protected ResponseHandler<T> second;


  public ResponseHandlerChain( ResponseHandler<HttpResponse> first,
    ResponseHandler<T> second )
  {
    this.first = first;
    this.second = second;
  }


  @Override
  public T handleResponse( HttpResponse httpResponse ) throws IOException
  {
    return second.handleResponse(first.handleResponse(httpResponse));
  }
}
