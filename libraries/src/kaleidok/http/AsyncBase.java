package kaleidok.http;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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


  public static class ResponseHandlerChain<T>
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


  protected static class ResponseMimeTypeChecker
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


  public static class ReaderResponseHandler
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
      Parsers.ContentType ct = Parsers.getContentType(httpResponse);
      Charset charset;
      if (ct != null && ct.charset != null) {
        charset = ct.charset;
      } else if (defaultCharset != null) {
        charset = defaultCharset;
      } else {
        throw new ClientProtocolException("Server returned invalid charset");
      }
      return new InputStreamReader(httpResponse.getEntity().getContent(), charset);
    }
  }
}
