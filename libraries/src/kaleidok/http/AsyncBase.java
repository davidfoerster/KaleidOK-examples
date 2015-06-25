package kaleidok.http;

import kaleidok.http.responsehandler.ResponseHandlerChain;
import kaleidok.http.responsehandler.ResponseMimeTypeChecker;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;


public abstract class AsyncBase
{
  protected final org.apache.http.client.fluent.Async underlying;

  public MimeTypeMap acceptedMimeTypes;


  public AsyncBase( MimeTypeMap acceptedMimeTypes )
  {
    this.acceptedMimeTypes = acceptedMimeTypes;
    underlying = org.apache.http.client.fluent.Async.newInstance();
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


  public AsyncBase use( org.apache.http.client.fluent.Executor executor )
  {
    underlying.use(executor);
    return this;
  }


  public AsyncBase use( java.util.concurrent.Executor concurrentExec )
  {
    underlying.use(concurrentExec);
    return this;
  }
}
