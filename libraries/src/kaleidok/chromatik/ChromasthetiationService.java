package kaleidok.chromatik;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Size;
import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.concurrent.GroupedThreadFactory;
import kaleidok.http.ImageAsync;
import kaleidok.http.JsonAsync;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import synesketch.emotion.EmotionalState;

import java.awt.Image;
import java.io.IOException;
import java.util.Collection;
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


  public ChromasthetiationService( Executor executor )
  {
    this.executor = executor;
    org.apache.http.client.fluent.Executor httpExecutor =
      org.apache.http.client.fluent.Executor.newInstance();
    jsonAsync = new JsonAsync().use(executor).use(httpExecutor);
    jsonAsync.gson = ChromatikQuery.gson;
    imageAsync = new ImageAsync().use(executor).use(httpExecutor);
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


  private class RunnableChromasthetiator extends KeywordChromasthetiator
    implements Runnable, FutureCallback<ChromatikResponse>
  {
    private final String text;

    private final FutureCallback<Future<Image>> futureImageCallback;

    private final FutureCallback<Image> imageCallback;

    private final AtomicInteger remainingCount;


    public RunnableChromasthetiator( ChromasthetiatorBase queryParams,
      String text, FutureCallback<Future<Image>> futureImageCallback,
      FutureCallback<Image> imageCallback, int maxCount )
    {
      super(queryParams);
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


    protected boolean takeTicket()
    {
      AtomicInteger remainingCount = this.remainingCount;
      if (remainingCount == null)
        return true;
      int n;
      do {
        n = remainingCount.get();
      } while (n > 0 && !remainingCount.compareAndSet(n, n - 1));
      assert n >= 0;
      return n > 0;
    }

    protected boolean hasTicket()
    {
      return remainingCount == null || remainingCount.get() > 0;
    }


    @Override
    public void completed( ChromatikResponse response )
    {
      for (ChromatikResponse.Result imgInfo: response.results) {
        final FlickrPhoto flickrPhoto = new FlickrPhoto(imgInfo);
        executor.execute(new Runnable()
        {
          @Override
          public void run()
          {
            try {
              boolean ticket = hasTicket();
              if (ticket) {
                // test image and prefetch available sizes
                Collection<Size> sizes = flickrPhoto.getSizesThrow(); // TODO: Re-implement using Async (?)
                assert sizes != null;
                ticket = takeTicket();
              }

              if (verbose >= 3) {
                System.out.println("Received " + (ticket ? 1 : 0) +
                  " downloading ticket for " + flickrPhoto.getMediumUrl());
              }

              if (ticket) {
                Future<Image> fImage = imageAsync.execute(
                  Request.Get(flickrPhoto.getLargestImageSize().getSource()),
                  imageCallback);
                if (futureImageCallback != null)
                  futureImageCallback.completed(fImage);
              } else {
                cancelled();
              }
            } catch (FlickrException ex) {
              failed(ex);
            }
          }
        });
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
