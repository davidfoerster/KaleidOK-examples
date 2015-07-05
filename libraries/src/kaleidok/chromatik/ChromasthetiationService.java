package kaleidok.chromatik;

import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.concurrent.GroupedThreadFactory;
import kaleidok.concurrent.NestedFutureCallback;
import kaleidok.flickr.AsyncFlickr;
import kaleidok.flickr.Size;
import kaleidok.flickr.internal.FlickrBase;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.SizeMap;
import kaleidok.http.ImageAsync;
import kaleidok.http.JsonAsync;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import synesketch.emotion.EmotionalState;

import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class ChromasthetiationService
{
  public static int DEFAULT_THREAD_POOL_SIZE = 4;

  private final Executor executor;

  private final JsonAsync jsonAsync;

  private final ImageAsync imageAsync;

  private final AsyncFlickr flickr;
  private boolean hasFlickrApiKey = false;


  public ChromasthetiationService( Executor executor )
  {
    this.executor = executor;
    org.apache.http.client.fluent.Executor httpExecutor =
      org.apache.http.client.fluent.Executor.newInstance();

    jsonAsync = new JsonAsync().use(executor).use(httpExecutor);
    jsonAsync.gson = ChromatikQuery.gson;

    imageAsync = new ImageAsync().use(executor).use(httpExecutor);

    flickr = new AsyncFlickr(null, null, executor, httpExecutor);
  }

  public ChromasthetiationService( int threadPoolSize )
  {
    this((threadPoolSize == 0) ?
      Executors.newCachedThreadPool(createThreadFactory()) :
      Executors.newFixedThreadPool(threadPoolSize, createThreadFactory()));
  }

  public ChromasthetiationService()
  {
    this(0);
  }

  private static ThreadFactory createThreadFactory()
  {
    return new GroupedThreadFactory("Chromastetiation", true);
  }


  public void submit( String text, ChromasthetiatorBase queryParams,
    FutureCallback<Future<Image>> futureImageCallback,
    FutureCallback<Image> imageCallback, int maxCount )
  {
    if (maxCount != 0) {
      executor.execute(
        new RunnableChromasthetiator(queryParams, text, futureImageCallback,
          imageCallback, maxCount));
    }
  }


  protected synchronized void setFlickrApiKey( FlickrBase flickr )
  {
    if (flickr.getApiKey() != null) {
      hasFlickrApiKey = true;
      this.flickr.setApiKey(flickr.getApiKey(), flickr.getApiSecret());
    }
  }


  private class RunnableChromasthetiator extends KeywordChromasthetiator<AsyncFlickr>
    implements Runnable, FutureCallback<ChromatikResponse>
  {
    private final String text;

    private final FutureCallback<Future<Image>> futureImageCallback;

    private final FutureCallback<Image> imageCallback;

    private final AtomicInteger remainingCount;


    public RunnableChromasthetiator( ChromasthetiatorBase<?> queryParams,
      String text, FutureCallback<Future<Image>> futureImageCallback,
      FutureCallback<Image> imageCallback, int maxCount )
    {
      super(queryParams);

      if (!hasFlickrApiKey)
        setFlickrApiKey(queryParams.flickr);
      flickr = ChromasthetiationService.this.flickr;

      this.text = text;
      this.futureImageCallback = futureImageCallback;
      this.imageCallback = imageCallback;
      remainingCount = (maxCount >= 0) ? new AtomicInteger(maxCount) : null;
    }


    @Override
    public void run()
    {
      EmotionalState emoState;
      try {
        emoState = synesthetiator.synesthetiseDirect(text);
      } catch (IOException ex) {
        if (futureImageCallback != null || imageCallback != null) {
          if (futureImageCallback != null)
            futureImageCallback.failed(ex);
          if (imageCallback != null)
            imageCallback.failed(ex);
        } else {
          ex.printStackTrace();
        }
        return;
      }

      chromatikQuery.keywords = getQueryKeywords(emoState);
      getQueryOptions(emoState, chromatikQuery.opts);

      jsonAsync.execute(Request.Get(chromatikQuery.getUri()),
        ChromatikResponse.class, this);
    }


    protected int takeTickets( int count )
    {
      AtomicInteger remainingCount = this.remainingCount;
      if (remainingCount == null)
        return count;
      int current, taken;
      do {
        current = remainingCount.get();
        if (current <= 0)
          return 0;
        taken =  Math.min(count, current);
      } while (!remainingCount.compareAndSet(current, current - taken));
      return taken;
    }

    protected boolean hasTickets( int count )
    {
      return remainingCount == null || remainingCount.get() >= count;
    }


    @Override
    public void completed( ChromatikResponse response )
    {
      for (ChromatikResponse.Result imgInfo: response.results) {
        final FlickrPhoto flickrPhoto = new FlickrPhoto(imgInfo);
        if (hasTickets(1)) {
          flickr.getPhotoSizes(flickrPhoto.id,
            new NestedFutureCallback<SizeMap, ChromatikResponse>(this)
            {
              @Override
              public void completed( SizeMap sizes )
              {
                flickrPhoto.setSizes(sizes);
                int ticket = takeTickets(1);
                if (verbose >= 3) {
                  System.out.println("Received " + ticket +
                    " download tickets for " + flickrPhoto.getMediumUrl());
                }
                if (ticket > 0) {
                  Future<Image> fImage = imageAsync.execute(
                    Request.Get(flickrPhoto.getLargestImageSize().source),
                    imageCallback);
                  if (futureImageCallback != null)
                    futureImageCallback.completed(fImage);
                } else {
                  cancelled();
                }
              }

              @Override
              public void failed( Exception ex )
              {
                if (ex instanceof FlickrException) {
                  ((FlickrException) ex).setPertainingObject(
                    flickrPhoto.getMediumUrl());
                } else if (ex instanceof IOException) {
                  Size s = flickrPhoto.getLargestImageSize();
                  String url = (s != null) ? s.source : flickrPhoto.getMediumUrl();
                  ex = new IOException("Couldn't load " + url, ex);
                }
                super.failed(ex);
              }
            });
        }
        imgInfo.flickrPhoto = flickrPhoto;
      }
    }

    @Override
    public void failed( Exception ex )
    {
      if (futureImageCallback != null)
        futureImageCallback.failed(ex);
      if (imageCallback != null)
        imageCallback.failed(ex);
    }

    @Override
    public void cancelled()
    {
      if (futureImageCallback != null)
        futureImageCallback.cancelled();
      if (imageCallback != null)
        imageCallback.cancelled();
    }
  }
}
