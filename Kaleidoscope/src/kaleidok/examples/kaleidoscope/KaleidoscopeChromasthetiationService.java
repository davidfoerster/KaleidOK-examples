package kaleidok.examples.kaleidoscope;

import kaleidok.chromatik.ChromasthetiationService;
import kaleidok.chromatik.ChromasthetiatorBase;
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

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.verbose;


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
    ChromasthetiatorBase.verbose = verbose;
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
    File cacheDir = new File((String) parent.getParameter(
      cacheParamBase + "path", parent.getClass().getCanonicalName()));
    if (!cacheDir.isAbsolute()) {
      cacheDir = new File(
        PlatformPaths.INSTANCE.getCacheDir().toString(),
        cacheDir.getPath());
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
      System.err.format("Warning: Couldn't set up an HTTP cache in \"%s\": %s%n",
        cacheDir, ex.getLocalizedMessage());
      if (debug >= 1)
        throw new AssertionError(ex);
      // else: use default cache storage
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
      assert image.getWidth(null) > 0 && image.getHeight(null) > 0;
      PImageFuture fImage = new PImageFuture(new PImage(image));

      LayerManager layers = parent.getLayers();
      int idx = imageListIndex.getAndIncrement() % (layers.size() + 1) - 1;
      if (idx >= 0) {
        layers.get(idx).setNextImage(fImage);
      } else {
        layers.bgImage = fImage;
      }
    }


    @Override
    public void failed( Exception ex )
    {
      if (ex == null) {
        System.out.println("Aborted!");
      } else if (ex instanceof FlickrException) {
        switch (((FlickrException) ex).getErrorCode()) {
        case 1: // Photo not found
        case 2: // Permission denied
          System.err.println(ex.getLocalizedMessage());
          return;
        }
      }
      super.failed(ex);
    }
  }
}
