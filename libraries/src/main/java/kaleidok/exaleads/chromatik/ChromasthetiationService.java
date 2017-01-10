package kaleidok.exaleads.chromatik;

import kaleidok.exaleads.chromatik.Chromasthetiator.FlickrPhoto;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.http.responsehandler.PImageBaseResponseHandler;
import kaleidok.util.Threads;
import kaleidok.flickr.*;
import kaleidok.util.concurrent.NestedFutureCallback;
import kaleidok.util.containers.BoundedCompletionQueue;
import kaleidok.http.async.ImageAsync;
import kaleidok.http.async.JsonAsync;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.stream.Collectors;

import static kaleidok.exaleads.chromatik.Chromasthetiator.EXPECTED_NEUTRAL_RESULT_COUNT;
import static kaleidok.exaleads.chromatik.Chromasthetiator.logger;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public class ChromasthetiationService
{
  public static int DEFAULT_THREAD_POOL_SIZE = 4;

  private final ExecutorService executor;

  private final JsonAsync jsonAsync;

  private final ImageAsync imageAsync;

  protected final FlickrAsync flickr;


  public ChromasthetiationService( ExecutorService executor,
    JsonAsync jsonAsync, ImageAsync imageAsync, FlickrAsync flickrAsync )
  {
    this.executor = executor;
    this.jsonAsync = jsonAsync;
    this.imageAsync = imageAsync;
    this.flickr = flickrAsync;
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


  protected void submit( String text,
    Chromasthetiator<FlickrAsync> chromasthetiator,
    FutureCallback<Future<Image>> futureImageCallback,
    FutureCallback<Pair<Image, Pair<ChromatikResponse, EmotionalState>>> imageCallback,
    Consumer<Collection<? super Photo>> imageQueueCompletionCallback,
    int maxCount )
  {
    if (maxCount != 0) {
      executor.execute(
        new Chromasthetiation(chromasthetiator, text, futureImageCallback,
          imageCallback, imageQueueCompletionCallback, maxCount));
    }
  }


  protected void setFlickrApiKey( String key, String secret )
  {
    if (key != null)
      flickr.setApiKey(key, secret);
  }


  private class Chromasthetiation
    implements Runnable, FutureCallback<Pair<ChromatikResponse, EmotionalState>>
  {
    private final Chromasthetiator<FlickrAsync> chromasthetiator;

    private final String text;

    private final FutureCallback<Future<Image>> futureImageCallback;

    private final FutureCallback<Pair<Image, Pair<ChromatikResponse, EmotionalState>>> imageCallback;

    private final BoundedCompletionQueue<Pair<Photo, Pair<ChromatikResponse, EmotionalState>>> photoQueue;


    /**
     * Constructs a {@link Runnable} wrapper around a {@link Chromasthetiator}
     * for a single asynchronous chromasthetiation action, e. g. to be used in
     * an {@link Executor}.
     *
     * @param chromasthetiator  The original chromasthetiator
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
    public Chromasthetiation(
      Chromasthetiator<FlickrAsync> chromasthetiator,
      String text, FutureCallback<Future<Image>> futureImageCallback,
      FutureCallback<Pair<Image, Pair<ChromatikResponse, EmotionalState>>> imageCallback,
      final Consumer<Collection<? super Photo>> imageQueueCompletionCallback,
      int maxCount )
    {
      this.chromasthetiator = chromasthetiator;
      this.text = text;
      this.futureImageCallback = futureImageCallback;
      this.imageCallback = imageCallback;
      photoQueue =
        new BoundedCompletionQueue<>(maxCount,
          chromasthetiator.getChromatikQuery().getNHits());
      if (imageQueueCompletionCallback != null)
      {
        photoQueue.completionCallback =
          ( objects ) -> imageQueueCompletionCallback.accept(
            objects.stream().map(Pair::getLeft).collect(Collectors.toList()));
      }
    }


    @Override
    public void run()
    {
      EmotionalState emoState;
      try
      {
        emoState = chromasthetiator.synesthetiator.synesthetiseDirect(text);
      }
      catch (IOException ex)
      {
        if (futureImageCallback != null)
          futureImageCallback.failed(ex);
        if (imageCallback != null)
          imageCallback.failed(ex);
        if (futureImageCallback == null && imageCallback == null)
          Threads.handleUncaught(ex);
        return;
      }
      //noinspection HardcodedLineSeparator
      logger.log(Level.FINE, "Synesthetiation result:\n{0}", emoState);

      ChromatikQuery chromatikQuery = chromasthetiator.getChromatikQuery();
      chromatikQuery.setKeywords(chromasthetiator.getQueryKeywords(emoState));

      Random textRandom = new Random(emoState.getText().hashCode());
      chromatikQuery.optionMap =
        chromasthetiator.getQueryOptions(
          emoState, chromatikQuery.optionMap, textRandom);

      int queryStart = chromatikQuery.getStart();
      if (chromatikQuery.getKeywords().isEmpty() &&
        emoState.getStrongestEmotion().getType() == Emotion.NEUTRAL)
      {
        textRandom.setSeed(emoState.getText().hashCode());
        chromatikQuery.randomizeRequestedSubset(
          EXPECTED_NEUTRAL_RESULT_COUNT, textRandom);
      }
      runChromatikQuery(emoState);
      chromatikQuery.setStart(queryStart);
    }


    private void runChromatikQuery( final EmotionalState emoState )
    {
      URI chromatikUri = chromasthetiator.getChromatikQuery().getUri();
      logger.log(Level.FINER,
        "Requesting search results from: {0}", chromatikUri);

      jsonAsync.execute(Request.Get(chromatikUri), ChromatikResponse.class,
        NestedFutureCallback.getInstance(this,
          (response, cb) -> cb.completed(Pair.of(response, emoState))));
    }


    @Override
    public void completed( Pair<ChromatikResponse, EmotionalState> o )
    {
      ChromatikResponse response = o.getLeft();
      logger.log(Level.FINE, "Chromatik found {0} results", response.hits);

      if (response.results.length == 0 &&
        !chromasthetiator.getChromatikQuery().getKeywords().isEmpty())
      {
        removeLastKeyword();
        runChromatikQuery(o.getRight());
        return;
      }

      Flickr flickr = chromasthetiator.flickr;
      synchronized (photoQueue) {
        for (ChromatikResponse.Result imgInfo : response.results) {
          final FlickrPhoto flickrPhoto =
            FlickrPhoto.fromChromatikResponseResult(flickr, imgInfo);
          imgInfo.flickrPhoto = flickrPhoto;
          photoQueue.add(Pair.of(flickrPhoto, o));
        }

        dispatchQueue();
      }
    }


    private void dispatchQueue()
    {
      FlickrAsync flickr = chromasthetiator.flickr;
      synchronized (photoQueue)
      {
        Pair<Photo, Pair<ChromatikResponse, EmotionalState>> o;
        while ((o = photoQueue.poll()) != null)
        {
          Photo photo = o.getLeft();
          logger.log(Level.FINEST,
            "Received a download ticket for {0}", photo);
          flickr.getPhotoSizes(
            photo.id, new PhotoSizesCallback(o));
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
      private final Pair<Photo, Pair<ChromatikResponse, EmotionalState>> previousResults;


      private PhotoSizesCallback(
        Pair<Photo, Pair<ChromatikResponse, EmotionalState>> previousResults )
      {
        this.previousResults = previousResults;
      }


      @Override
      public void completed( SizeMap sizes )
      {
        Photo photo = previousResults.getLeft();
        photo.setSizes(sizes);
        Future<Image> fImage = imageAsync.execute(
          Request.Get(photo.getLargestImageSize().source),
          new ImageCallbackWrapper(previousResults));

        if (futureImageCallback != null)
          futureImageCallback.completed(fImage);
      }


      @Override
      public void failed( Exception ex )
      {
        if (ex instanceof FlickrException) {
          ((FlickrException) ex).setPertainingObject(
            previousResults.getLeft().getMediumUrl());
        } else if (ex instanceof IOException) {
          ex = new IOException(
            "Couldn't load sizes of " + previousResults.getLeft(), ex);
        }

        Chromasthetiation.this.failed(ex);
        releaseQueuePermit();
      }


      @Override
      public void cancelled()
      {
        Chromasthetiation.this.cancelled();
        releaseQueuePermit();
      }
    }


    private final class ImageCallbackWrapper implements FutureCallback<Image>
    {
      private final Pair<Photo, Pair<ChromatikResponse, EmotionalState>> previousResults;


      private ImageCallbackWrapper(
        Pair<Photo, Pair<ChromatikResponse, EmotionalState>> previousResults )
      {
        this.previousResults = previousResults;
      }


      private Photo getPhoto()
      {
        return previousResults.getLeft();
      }


      @Override
      public void completed( Image image )
      {
        logger.log(Level.FINE, "Downloaded image {0}",
          getPhoto().getLargestImageSize().source);
        photoQueue.completeItem();
        imageCallback.completed(Pair.of(image, previousResults.getRight()));
      }

      @Override
      public void failed( Exception ex )
      {
        logThrown(logger,
          (ex instanceof IOException) ? Level.SEVERE : Level.FINER,
          "Couldn''t download {0}",
          ex, getPhoto().getLargestImageSize().source);

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
      ChromatikQuery chromatikQuery = chromasthetiator.getChromatikQuery();
      String keywords = chromatikQuery.getKeywords();
      int p = keywords.lastIndexOf(' ');
      while (p > 0 && keywords.charAt(p - 1) == ' ')
        p--;
      chromatikQuery.setKeywords((p > 0) ? keywords.substring(0, p) : "");
    }
  }
}
