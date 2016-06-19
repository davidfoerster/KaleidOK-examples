package kaleidok.http.async;

import kaleidok.http.responsehandler.ResponseHandlerChain;
import kaleidok.http.responsehandler.ResponseMimeTypeChecker;
import kaleidok.http.util.MimeTypeMap;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;


public abstract class AsyncBase
{
  protected final org.apache.http.client.fluent.Async underlying;

  public MimeTypeMap acceptedMimeTypes;


  protected AsyncBase( org.apache.http.client.fluent.Async fluentAsync,
    MimeTypeMap acceptedMimeTypes )
  {
    this.underlying = fluentAsync;
    this.acceptedMimeTypes = acceptedMimeTypes;
  }


  protected void applyAcceptedMimeTypes( Request request )
  {
    if (acceptedMimeTypes != null) {
      String accept = acceptedMimeTypes.toString();
      if (!accept.isEmpty())
        request.addHeader(HttpHeaders.ACCEPT, accept);
    }
  }


  public abstract Future<?> execute( Request request );


  protected <T> Future<T> execute( Request request,
    ResponseHandler<T> handler )
  {
    return execute(request, handler, null);
  }

  protected <T> Future<T> execute( Request request,
    ResponseHandler<T> handler, FutureCallback<T> callback )
  {
    applyAcceptedMimeTypes(request);
    return underlying.execute(request,
      new ResponseHandlerChain<>(
        new ResponseMimeTypeChecker(acceptedMimeTypes),
        handler),
      callback);
  }
}
