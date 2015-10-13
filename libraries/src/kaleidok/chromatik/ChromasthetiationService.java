package kaleidok.chromatik;

import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.concurrent.GroupedThreadFactory;
import kaleidok.util.BoundedCompletionQueue;
import kaleidok.flickr.AsyncFlickr;
import kaleidok.flickr.internal.FlickrBase;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.SizeMap;
import kaleidok.http.async.ImageAsync;
import kaleidok.http.async.JsonAsync;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import synesketch.emotion.EmotionalState;

import java.awt.Image;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;
import java.util.logging.Level;


public class ChromasthetiationService
{
  public static int DEFAULT_THREAD_POOL_SIZE = 4;

  private final Executor executor;

  private final JsonAsync jsonAsync;

  private final ImageAsync imageAsync;

  private final AsyncFlickr flickr;
  private boolean hasFlickrApiKey = false;


  public ChromasthetiationService( Executor executor,
    org.apache.http.client.fluent.Executor httpExecutor )
  {
    this.executor = executor;

    jsonAsync = new JsonAsync().use(executor).use(httpExecutor);
    jsonAsync.gson = ChromatikQuery.gson;

    imageAsync = new ImageAsync().use(executor).use(httpExecutor);

    flickr = new AsyncFlickr(null, null, executor, httpExecutor);
  }


  public ChromasthetiationService( Executor executor )
  {
    this(executor, org.apache.http.client.fluent.Executor.newInstance());
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
    return new GroupedThreadFactory("Chromasthetiation", true);
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

    private final BoundedCompletionQueue<FlickrPhoto> photoQueue;


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
      photoQueue = new BoundedCompletionQueue<>(maxCount, chromatikQuery.nhits);
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
      logger.log(Level.FINE, "Synesthetiation result:\n{0}", emoState);

      chromatikQuery.keywords = getQueryKeywords(emoState);
      getQueryOptions(emoState, chromatikQuery.opts);
      runChromatikQuery();
    }


    private void runChromatikQuery()
    {
      URI chromatikUri = chromatikQuery.getUri();
      logger.log(Level.FINER,
        "Requesting search results from: {0}", chromatikUri);

      jsonAsync.execute(Request.Get(chromatikUri),
        ChromatikResponse.class, this);
    }


    @Override
    public void completed( ChromatikResponse response )
    {
      logger.log(Level.FINE, "Chromatik found {0} results", response.hits);

      if (response.results.length == 0 && !chromatikQuery.keywords.isEmpty()) {
        removeLastKeyword();
        runChromatikQuery();
        return;
      }

      synchronized (photoQueue) {
        for (ChromatikResponse.Result imgInfo : response.results) {
          final FlickrPhoto flickrPhoto = new FlickrPhoto(imgInfo);
          imgInfo.flickrPhoto = flickrPhoto;
          photoQueue.add(flickrPhoto);
        }

        dispatchQueue();
      }
    }


    private void dispatchQueue()
    {
      synchronized (photoQueue) {
        FlickrPhoto flickrPhoto;
        while ((flickrPhoto = photoQueue.poll()) != null) {
          logger.log(Level.FINEST,
            "Received a download ticket for {0}", flickrPhoto);
          flickr.getPhotoSizes(
            flickrPhoto.id, new PhotoSizesCallback(flickrPhoto));
        }
      }
    }


    private void releaseQueuePermit()
    {
      synchronized (photoQueue) {
        photoQueue.release();
        dispatchQueue();
      }
    }


    private class PhotoSizesCallback implements FutureCallback<SizeMap>
    {
      private final FlickrPhoto flickrPhoto;


      private PhotoSizesCallback( FlickrPhoto flickrPhoto )
      {
        this.flickrPhoto = flickrPhoto;
      }


      @Override
      public void completed( SizeMap sizes )
      {
        flickrPhoto.setSizes(sizes);
        Future<Image> fImage = imageAsync.execute(
          Request.Get(flickrPhoto.getLargestImageSize().source),
          new FutureCallback<Image>()
          {
            @Override
            public void completed( Image image )
            {
              logger.log(Level.FINE, "Downloaded image {0}",
                flickrPhoto.getLargestImageSize().source);
              photoQueue.completeItem();
              imageCallback.completed(image);
            }

            @Override
            public void failed( Exception ex )
            {
              imageCallback.failed(ex);
              releaseQueuePermit();
            }

            @Override
            public void cancelled()
            {
              imageCallback.cancelled();
              releaseQueuePermit();
            }
          });

        if (futureImageCallback != null)
          futureImageCallback.completed(fImage);
      }


      @Override
      public void failed( Exception ex )
      {
        if (ex instanceof FlickrException) {
          ((FlickrException) ex).setPertainingObject(
            flickrPhoto.getMediumUrl());
        } else if (ex instanceof IOException) {
          ex = new IOException("Couldn't load sizes of " + flickrPhoto, ex);
        }

        RunnableChromasthetiator.this.failed(ex);
        releaseQueuePermit();
      }


      @Override
      public void cancelled()
      {
        RunnableChromasthetiator.this.cancelled();
        releaseQueuePermit();
      }
    }


    @Override
    public void failed( Exception ex )
    {
      if (futureImageCallback != null)
        futureImageCallback.failed(ex);
    }


    @Override
    public void cancelled()
    {
      if (futureImageCallback != null)
        futureImageCallback.cancelled();
    }


    private void removeLastKeyword()
    {
      /*
       * Due to sorting, the last keyword has simultaneously the weakest
       * emotional weight.
       */
      String keywords = chromatikQuery.keywords;
      int p = keywords.lastIndexOf(' ');
      chromatikQuery.keywords = (p > 0) ? keywords.substring(0, p) : "";
    }
  }
}
