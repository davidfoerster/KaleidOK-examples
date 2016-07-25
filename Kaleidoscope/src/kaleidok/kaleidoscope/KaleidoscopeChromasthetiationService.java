package kaleidok.kaleidoscope;

import kaleidok.exaleads.chromatik.ChromasthetiationService;
import kaleidok.exaleads.chromatik.DocumentChromasthetiator;
import kaleidok.processing.image.PImages;
import kaleidok.util.concurrent.AbstractFutureCallback;
import kaleidok.util.concurrent.GroupedThreadFactory;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.Photo;
import kaleidok.http.cache.DiskLruHttpCacheStorage;
import kaleidok.http.cache.ExecutorSchedulingStrategy;
import kaleidok.io.platform.PlatformPaths;
import kaleidok.processing.image.PImageFuture;
import kaleidok.util.DefaultValueParser;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.LoggingUtils.logThrown;


public final class KaleidoscopeChromasthetiationService
  extends ChromasthetiationService
{
  @SuppressWarnings("HardcodedLineSeparator")
  private static final Pattern KEY_SEPARATOR_PATTERN =
    Pattern.compile(":|\r?\n");

  public static long DEFAULT_HTTP_CACHE_SIZE = 50L << 20;

  private static final int HTTP_CACHE_APP_VERSION = 1;


  private final Kaleidoscope parent;

  private DocumentChromasthetiator<Flickr> chromasthetiator;

  private final ChromasthetiationCallback chromasthetiationCallback =
    new ChromasthetiationCallback();

  private ImageQueueCompletionCallback imageQueueCompletionCallback = null;


  private KaleidoscopeChromasthetiationService( Kaleidoscope parent,
    ExecutorService executor, Executor httpExecutor )
  {
    super(executor, Async.newInstance().use(executor).use(httpExecutor));
    this.parent = parent;
    //parent.registerMethod("dispose", this);
  }


  @SuppressWarnings("unused")
  public void dispose()
  {
    shutdown();
  }


  static KaleidoscopeChromasthetiationService newInstance( Kaleidoscope parent )
  {
    Map<String, String> parameters = parent.getParameterMap();

    int threadPoolSize = DefaultValueParser.parseInt(
      parameters.get(
        ChromasthetiationService.class.getCanonicalName() + ".threads"),
      ChromasthetiationService.DEFAULT_THREAD_POOL_SIZE);

    String cacheParamBase = parent.getClass().getCanonicalName() + ".cache.";
    long httpCacheSize = DefaultValueParser.parseLong(
      parameters.get(cacheParamBase + "size"), DEFAULT_HTTP_CACHE_SIZE);
    File cacheDir = new File(parent.getParameterMap().getOrDefault(
      cacheParamBase + "path", parent.getClass().getCanonicalName()));
    if (!cacheDir.isAbsolute()) {
      cacheDir =
        PlatformPaths.getCacheDir().resolve(cacheDir.getPath()).toFile();
    }

    CachingHttpClientBuilder builder = CachingHttpClientBuilder.create();
    builder
      .disableConnectionState()
      .disableCookieManagement();

    ThreadFactory threadFactory =
      new GroupedThreadFactory("Chromasthetiation", true);
    ExecutorService executor = (threadPoolSize == 0) ?
      Executors.newCachedThreadPool(threadFactory) :
      Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    builder.setSchedulingStrategy(
      new ExecutorSchedulingStrategy(executor));

    CacheConfig cacheConfig = CacheConfig.custom()
      .setMaxCacheEntries(Integer.MAX_VALUE)
      .setMaxObjectSize(httpCacheSize / 2)
      .setSharedCache(false)
      .setAllow303Caching(true)
      .setHeuristicCachingEnabled(true)
      .setHeuristicCoefficient(0.5f)
      .setHeuristicDefaultLifetime(TimeUnit.DAYS.toSeconds(28))
      .setAsynchronousWorkersCore(0)
      .build();

    try {
      builder.setHttpCacheStorage(new DiskLruHttpCacheStorage(
        cacheDir, HTTP_CACHE_APP_VERSION, httpCacheSize));
    } catch (IOException ex) {
      String msg = "Couldn't set up an HTTP cache";
      if (KaleidoscopeChromasthetiationService.class.desiredAssertionStatus())
        throw new AssertionError(msg, ex);

      // else: use default cache storage
      logThrown(logger, Level.WARNING, "{0} in \"{1}\"", ex,
        new Object[]{msg, cacheDir});
    }

    builder.setCacheConfig(cacheConfig);
    return new KaleidoscopeChromasthetiationService(parent, executor,
      Executor.newInstance(builder.build()));
  }


  private DocumentChromasthetiator<Flickr> getChromasthetiator()
  {
    if (chromasthetiator == null)
    {
      chromasthetiator = new DocumentChromasthetiator<>();

      String data = parent.parseStringOrFile(
        parent.getParameterMap().get("com.flickr.api.key"), '@');
      if (data != null) {
        String[] keys = KEY_SEPARATOR_PATTERN.split(data, 2);
        if (keys.length != 2) {
          throw new IllegalArgumentException(
            "Malformed Flickr API key: " + data);
        }
        Flickr flickr = new Flickr();
        flickr.setApiKey(keys[0], keys[1]);
        chromasthetiator.setFlickrApi(flickr);
      }

      chromasthetiator.maxKeywords = DefaultValueParser.parseInt(
        parent.getParameterMap().get(
          chromasthetiator.getClass().getPackage().getName() + ".maxKeywords"),
        chromasthetiator.maxKeywords);

      chromasthetiator.chromatikQuery.nHits = 10;
    }
    return chromasthetiator;
  }


  public void submit( final String text )
  {
    Consumer<Collection<? super Photo>> imageQueueCompletionCallback =
      (this.imageQueueCompletionCallback != null) ?
        (photos) -> this.imageQueueCompletionCallback.accept(text, photos) :
        null;

    submit(text, getChromasthetiator(),
      null, chromasthetiationCallback, imageQueueCompletionCallback,
      parent.getLayers().size());
  }


  private class ChromasthetiationCallback
    extends AbstractFutureCallback<Image>
  {
    private final AtomicInteger imageListIndex = new AtomicInteger(0);


    @Override
    public void completed( Image image )
    {
      assert image.getWidth(null) > 0 && image.getHeight(null) > 0 :
        image + " has width or height ≤0";
      PImageFuture fImage = PImageFuture.from(PImages.from(image));

      LayerManager layers = parent.getLayers();
      int idx = imageListIndex.getAndIncrement() % layers.size();
      layers.get(idx).setNextImage(fImage);
    }


    @Override
    public void failed( Exception ex )
    {
      if (ex == null)
      {
        logger.fine("Chromasthetiation aborted");
      }
      else if (ex instanceof FlickrException)
      {
        switch (((FlickrException) ex).getErrorCode())
        {
        case 1: // Photo not found
        case 2: // Permission denied
          logger.log(Level.FINE, "Flickr says: {0}", ex.getLocalizedMessage());
          return;
        }
      }
      super.failed(ex);
    }
  }


  public interface ImageQueueCompletionCallback
  {
    void accept( String text, Collection<? super Photo> photos );
  }


  public void setImageQueueCompletionCallback(
    ImageQueueCompletionCallback imageQueueCompletionCallback )
  {
    this.imageQueueCompletionCallback = imageQueueCompletionCallback;
  }


  public void setImageQueueCompletionCallback(
    final Consumer<String> imageQueueCompletionCallback )
  {
    setImageQueueCompletionCallback(
      (imageQueueCompletionCallback != null) ?
        ( text, photos ) -> imageQueueCompletionCallback.accept(text) :
        null);
  }
}
