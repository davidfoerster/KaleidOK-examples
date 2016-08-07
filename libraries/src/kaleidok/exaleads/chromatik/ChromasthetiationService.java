package kaleidok.exaleads.chromatik;

import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.http.responsehandler.PImageBaseResponseHandler;
import kaleidok.util.concurrent.GroupedThreadFactory;
import kaleidok.flickr.*;
import kaleidok.util.containers.BoundedCompletionQueue;
import kaleidok.http.async.ImageAsync;
import kaleidok.http.async.JsonAsync;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;

import java.awt.Image;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;

import static kaleidok.util.logging.LoggingUtils.logThrown;


public class ChromasthetiationService
{
  public static int DEFAULT_THREAD_POOL_SIZE = 4;

  private final ExecutorService executor;

  private final JsonAsync jsonAsync;

  private final ImageAsync imageAsync;

  protected final FlickrAsync flickrAsync;


  public ChromasthetiationService( ExecutorService executor,
    JsonAsync jsonAsync, ImageAsync imageAsync, FlickrAsync flickrAsync )
  {
    this.executor = executor;
    this.jsonAsync = jsonAsync;
    this.imageAsync = imageAsync;
    this.flickrAsync = flickrAsync;
  }


  public ChromasthetiationService( ExecutorService executor,
    JsonAsync jsonAsync, ImageAsync imageAsync )
  {
    this(executor, jsonAsync, imageAsync, new FlickrAsync(jsonAsync));
  }


  public ChromasthetiationService( ExecutorService executor, Async fluentAsync )
  {
    this(executor, new JsonAsync(fluentAsync),
      new ImageAsync(fluentAsync, PImageBaseResponseHandler.INSTANCE));
  }


  public void shutdown()
  {
    executor.shutdown();
  }


  private static ThreadFactory createThreadFactory()
  {
    return new GroupedThreadFactory("Chromasthetiation", true);
  }


  public void submit( String text,
    ChromasthetiatorBase<? extends Flickr> queryParams,
    FutureCallback<Future<Image>> futureImageCallback,
    FutureCallback<Image> imageCallback,
    Consumer<Collection<? super Photo>> imageQueueCompletionCallback,
    int maxCount )
  {
    if (maxCount != 0) {
      executor.execute(
        new RunnableChromasthetiator(queryParams, text, futureImageCallback,
          imageCallback, imageQueueCompletionCallback, maxCount));
    }
  }


  protected void setFlickrApiKey( String key, String secret )
  {
    if (key != null)
      flickrAsync.setApiKey(key, secret);
  }


  private class RunnableChromasthetiator
    extends KeywordChromasthetiator<FlickrAsync>
    implements Runnable, FutureCallback<ChromatikResponse>
  {
    private final String text;

    private final FutureCallback<Future<Image>> futureImageCallback;

    private final FutureCallback<Image> imageCallback;

    private final BoundedCompletionQueue<Photo> photoQueue;


    /**
     * Constructs a new chromasthetiator based on the parent applet and
     * parameters like
     * {@link ChromasthetiatorBase#ChromasthetiatorBase(ChromasthetiatorBase)}
     * and adds the source string as well as various callback objects to allow
     * it to be used as a {@link Runnable}, e. g. in an {@link Executor}.
     *
     * @param queryParams  The original chromasthetiator
     * @param text  The text to chromasthetiate
     * @param futureImageCallback  A callback that provides a {@link Future} to
     *   the images passed to {@link #imageCallback}
     * @param imageCallback  A callback for each of the images that are going
     *   to fetched as a result of this chromasthetiation
     * @param imageQueueCompletionCallback  A callback that completes once
     *   <em>all actually available</em> images have been fetched (at most
     *   {@code maxCount})
     * @param maxCount  Fetch up to this many images from the chromasthetiation
     *   result
     */
    public RunnableChromasthetiator(
      ChromasthetiatorBase<? extends Flickr> queryParams,
      String text, FutureCallback<Future<Image>> futureImageCallback,
      FutureCallback<Image> imageCallback,
      Consumer<Collection<? super Photo>> imageQueueCompletionCallback,
      int maxCount )
    {
      super(queryParams);

      flickr = ChromasthetiationService.this.flickrAsync;
      if (flickr.getApiKey() == null)
      {
        setFlickrApiKey(queryParams.flickr.getApiKey(),
          queryParams.flickr.getApiSecret());
      }

      this.text = text;
      this.futureImageCallback = futureImageCallback;
      this.imageCallback = imageCallback;
      photoQueue = new BoundedCompletionQueue<>(maxCount, chromatikQuery.nHits);
      photoQueue.completionCallback = imageQueueCompletionCallback;
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
      //noinspection HardcodedLineSeparator
      logger.log(Level.FINE, "Synesthetiation result:\n{0}", emoState);

      chromatikQuery.keywords = getQueryKeywords(emoState);

      Random textRandom = new Random(emoState.getText().hashCode());
      getQueryOptions(emoState, chromatikQuery.opts, textRandom);

      int queryStart = chromatikQuery.start;
      if (chromatikQuery.keywords.isEmpty() &&
        emoState.getStrongestEmotion().getType() == Emotion.NEUTRAL)
      {
        textRandom.setSeed(emoState.getText().hashCode());
        chromatikQuery.randomizeRequestedSubset(
          EXPECTED_NEUTRAL_RESULT_COUNT, textRandom);
      }
      runChromatikQuery();
      chromatikQuery.start = queryStart;
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

      Flickr flickr = this.flickr;
      synchronized (photoQueue) {
        for (ChromatikResponse.Result imgInfo : response.results) {
          final FlickrPhoto flickrPhoto =
            FlickrPhoto.fromChromatikResponseResult(flickr, imgInfo);
          imgInfo.flickrPhoto = flickrPhoto;
          photoQueue.add(flickrPhoto);
        }

        dispatchQueue();
      }
    }


    private void dispatchQueue()
    {
      FlickrAsync flickr = this.flickr;
      synchronized (photoQueue) {
        Photo photo;
        while ((photo = photoQueue.poll()) != null) {
          logger.log(Level.FINEST,
            "Received a download ticket for {0}", photo);
          flickr.getPhotoSizes(
            photo.id, new PhotoSizesCallback(photo));
        }
      }
    }


    private void releaseQueuePermit()
    {
      /*
       * TODO: Wait until all pictures are actually displayed on the screen so
       * they appear in a screenshot at that time.
       */
      synchronized (photoQueue) {
        photoQueue.release();
        dispatchQueue();
      }
    }


    private final class PhotoSizesCallback implements FutureCallback<SizeMap>
    {
      private final Photo photo;


      private PhotoSizesCallback( Photo photo )
      {
        this.photo = photo;
      }


      @Override
      public void completed( SizeMap sizes )
      {
        photo.setSizes(sizes);
        Future<Image> fImage = imageAsync.execute(
          Request.Get(photo.getLargestImageSize().source),
          new ImageCallback(photo));

        if (futureImageCallback != null)
          futureImageCallback.completed(fImage);
      }


      @Override
      public void failed( Exception ex )
      {
        if (ex instanceof FlickrException) {
          ((FlickrException) ex).setPertainingObject(
            photo.getMediumUrl());
        } else if (ex instanceof IOException) {
          ex = new IOException("Couldn't load sizes of " + photo, ex);
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


    private final class ImageCallback implements FutureCallback<Image>
    {
      private final Photo photo;


      private ImageCallback( Photo photo )
      {
        this.photo = photo;
      }


      @Override
      public void completed( Image image )
      {
        logger.log(Level.FINE, "Downloaded image {0}",
          photo.getLargestImageSize().source);
        photoQueue.completeItem();
        imageCallback.completed(image);
      }

      @Override
      public void failed( Exception ex )
      {
        logThrown(logger,
          (ex instanceof IOException) ? Level.SEVERE : Level.FINER,
          "Couldn't download {0}",
          ex, photo.getLargestImageSize().source);

        imageCallback.failed(ex);
        releaseQueuePermit();
      }

      @Override
      public void cancelled()
      {
        imageCallback.cancelled();
        releaseQueuePermit();
      }
    }


    @Override
    public void failed( Exception ex )
    {
      logger.log(
        (ex instanceof IOException) ? Level.SEVERE : Level.FINER,
        "Chromasthetiation error", ex);

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
