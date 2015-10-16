package kaleidok.examples.kaleidoscope;

import kaleidok.chromatik.ChromasthetiationService;
import kaleidok.chromatik.DocumentChromasthetiator;
import kaleidok.concurrent.AbstractFutureCallback;
import kaleidok.concurrent.GroupedThreadFactory;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.http.cache.DiskLruHttpCacheStorage;
import kaleidok.http.cache.ExecutorSchedulingStrategy;
import kaleidok.io.platform.PlatformPaths;
import kaleidok.processing.PImageFuture;
import kaleidok.util.DefaultValueParser;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import processing.core.PImage;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.LoggingUtils.logThrown;


public class KaleidoscopeChromasthetiationService extends ChromasthetiationService
{
  public static long DEFAULT_HTTP_CACHE_SIZE = 50L << 20;

  private static final int HTTP_CACHE_APP_VERSION = 1;


  private final Kaleidoscope parent;

  private DocumentChromasthetiator chromasthetiator;

  private final ChromasthetiationCallback chromasthetiationCallback =
    new ChromasthetiationCallback();


  private KaleidoscopeChromasthetiationService( Kaleidoscope parent,
    java.util.concurrent.Executor executor,
    org.apache.http.client.fluent.Executor httpExecutor )
  {
    super(executor, httpExecutor);
    this.parent = parent;
  }


  static KaleidoscopeChromasthetiationService newInstance( Kaleidoscope parent )
  {
    int threadPoolSize = DefaultValueParser.parseInt(parent,
      ChromasthetiationService.class.getCanonicalName() + ".threads",
      ChromasthetiationService.DEFAULT_THREAD_POOL_SIZE);

    String cacheParamBase = parent.getClass().getCanonicalName() + ".cache.";
    long httpCacheSize = DefaultValueParser.parseLong(parent,
      cacheParamBase + "size", DEFAULT_HTTP_CACHE_SIZE);
    File cacheDir = new File(parent.getParameter(
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
      if (debug >= 1)
        throw new AssertionError(msg, ex);

      // else: use default cache storage
      logThrown(logger, Level.WARNING, "{0} in \"{1}\"", ex,
        new Object[]{msg, cacheDir});
    }

    builder.setCacheConfig(cacheConfig);
    return new KaleidoscopeChromasthetiationService(parent, executor,
      org.apache.http.client.fluent.Executor.newInstance(builder.build()));
  }


  private DocumentChromasthetiator getChromasthetiator()
  {
    if (chromasthetiator == null)
    {
      chromasthetiator = new DocumentChromasthetiator(parent);

      String data = parent.parseStringOrFile(
        parent.getParameter("com.flickr.api.key"), '@');
      if (data != null) {
        String[] keys = data.split(":|\r?\n", 2);
        if (keys.length != 2) {
          throw new IllegalArgumentException(
            "Malformed Flickr API key: " + data);
        }
        chromasthetiator.setFlickrApi(new Flickr(keys[0], keys[1]));
      }

      chromasthetiator.maxKeywords = DefaultValueParser.parseInt(parent,
        chromasthetiator.getClass().getPackage().getName() + ".maxKeywords",
        chromasthetiator.maxKeywords);

      chromasthetiator.chromatikQuery.nhits = 10;
    }
    return chromasthetiator;
  }


  public void submit( String text )
  {
    submit(text, getChromasthetiator(),
      null, chromasthetiationCallback, parent.getLayers().size() + 1);
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
      PImageFuture fImage = new PImageFuture(new PImage(image));

      LayerManager layers = parent.getLayers();
      int idx = imageListIndex.getAndIncrement() % (layers.size() + 1) - 1;
      layers.get(idx).setNextImage(fImage);
    }


    @Override
    public void failed( Exception ex )
    {
      if (ex == null) {
        logger.fine("Chromasthetiation aborted");
      } else if (ex instanceof FlickrException) {
        switch (((FlickrException) ex).getErrorCode()) {
        case 1: // Photo not found
        case 2: // Permission denied
          logger.log(Level.FINE, "Flickr says: {0}", ex.getLocalizedMessage());
          return;
        }
      }
      super.failed(ex);
    }
  }
}
