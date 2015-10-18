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
  public Async( org.apache.http.client.fluent.Async fluentAsync,
    MimeTypeMap acceptedMimeTypes )
  {
    super(fluentAsync, acceptedMimeTypes);
  }

  public Async( org.apache.http.client.fluent.Async fluentAsync )
  {
    this(fluentAsync, new MimeTypeMap());
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
