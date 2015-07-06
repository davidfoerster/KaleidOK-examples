package kaleidok.http.async;


import kaleidok.http.util.MimeTypeMap;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.ContentResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;


public class Async extends AsyncBase
{
  public Async( MimeTypeMap acceptedMimeTypes )
  {
    super(acceptedMimeTypes);
  }

  public Async()
  {
    this(new MimeTypeMap());
  }

  @Override
  public Async use( org.apache.http.client.fluent.Executor executor )
  {
    super.use(executor);
    return this;
  }

  @Override
  public Async use( java.util.concurrent.Executor concurrentExec )
  {
    super.use(concurrentExec);
    return this;
  }

  public Future<Content> execute( Request request )
  {
    return execute(request, (FutureCallback<Content>) null);
  }

  public Future<Content> execute( Request request,
    FutureCallback<Content> callback )
  {
    return execute(request, new ContentResponseHandler(), callback);
  }

  @Override
  public <T> Future<T> execute( Request request, ResponseHandler<T> handler )
  {
    return super.execute(request, handler);
  }

  @Override
  public <T> Future<T> execute( Request request, ResponseHandler<T> handler,
    FutureCallback<T> callback )
  {
    return super.execute(request, handler, callback);
  }
}
