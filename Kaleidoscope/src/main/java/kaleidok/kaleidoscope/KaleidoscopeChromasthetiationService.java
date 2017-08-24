package kaleidok.kaleidoscope;

import javafx.beans.property.ObjectProperty;
import javafx.util.StringConverter;
import kaleidok.exaleads.chromatik.ChromasthetiationService;
import kaleidok.exaleads.chromatik.Chromasthetiator;
import kaleidok.exaleads.chromatik.PropertyChromasthetiator;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.flickr.FlickrAsync;
import kaleidok.image.filter.HSBImageFilter;
import kaleidok.image.filter.RGBImageFilterFormat;
import kaleidok.javafx.beans.property.AspectedObjectProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.StringConversionPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.util.converter.CachingFormattedStringConverter;
import kaleidok.processing.image.PImages;
import kaleidok.util.concurrent.AbstractFutureCallback;
import kaleidok.util.concurrent.GroupedThreadFactory;
import kaleidok.flickr.FlickrException;
import kaleidok.flickr.Photo;
import kaleidok.net.http.cache.DiskLruHttpCacheStorage;
import kaleidok.net.http.cache.ExecutorSchedulingStrategy;
import kaleidok.io.platform.PlatformPaths;
import kaleidok.util.prefs.DefaultValueParser;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import processing.core.PImage;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;

import java.awt.Image;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public final class KaleidoscopeChromasthetiationService
  extends ChromasthetiationService implements PreferenceBean
{
  @SuppressWarnings("HardcodedLineSeparator")
  private static final Pattern KEY_SEPARATOR_PATTERN =
    Pattern.compile(":|\r?\n");

  public static long DEFAULT_HTTP_CACHE_SIZE = 50L << 20;

  private static final int HTTP_CACHE_APP_VERSION = 1;


  private final Kaleidoscope parent;

  private PropertyChromasthetiator<FlickrAsync> chromasthetiator;

  private final ChromasthetiationCallback chromasthetiationCallback =
    new ChromasthetiationCallback();

  public BiConsumer<String, Stream<? super Photo>> imageQueueCompletionCallback = null;

  private final AspectedObjectProperty<RGBImageFilter> neutralFilter;


  private KaleidoscopeChromasthetiationService( Kaleidoscope parent,
    ExecutorService executor, Executor httpExecutor )
  {
    super(executor, Async.newInstance().use(executor).use(httpExecutor));
    this.parent = parent;

    neutralFilter = new AspectedObjectProperty<>(this, "neutral image filter");
    StringConverter<RGBImageFilter> filterConverter =
      new CachingFormattedStringConverter<>(new RGBImageFilterFormat());
    neutralFilter.addAspect(StringConverterAspectTag.getInstance(),
      filterConverter);
    neutralFilter.addAspect(PropertyPreferencesAdapterTag.getInstance(),
      new StringConversionPropertyPreferencesAdapter<>(neutralFilter, filterConverter));
    loadDefaultNeutralFilter();
  }


  public void dispose()
  {
    shutdown();
  }


  static KaleidoscopeChromasthetiationService newInstance( Kaleidoscope parent )
  {
    String[] aKey = getFlickrApiKey(parent);

    HttpClientBuilder builder = getCachingHttpClientBuilder(parent);
    if (builder == null)
    {
      // fall back
      builder = HttpClientBuilder.create();
    }
    builder
      .disableConnectionState()
      .disableCookieManagement();

    ExecutorService executor = getExecutor(parent.getParameterMap());
    if (builder instanceof CachingHttpClientBuilder)
    {
      ((CachingHttpClientBuilder) builder).setSchedulingStrategy(
        new ExecutorSchedulingStrategy(executor));
    }

    KaleidoscopeChromasthetiationService chromasthetiationService =
      new KaleidoscopeChromasthetiationService(parent, executor,
        Executor.newInstance(builder.build()));

    if (aKey != null)
      chromasthetiationService.flickr.setApiKey(aKey[0], aKey[1]);

    //noinspection SpellCheckingInspection
    String sMaxKeyWords = parent.getParameterMap().get(
      Chromasthetiator.class.getPackage().getName() + ".maxkeywords");
    if (sMaxKeyWords != null)
    {
      chromasthetiationService.getChromasthetiator()
        .setMaxKeywords(Integer.parseInt(sMaxKeyWords));
    }

    return chromasthetiationService;
  }


  private static String[] getFlickrApiKey( Kaleidoscope parent )
  {
    String sKey = parent.parseStringOrFile(
      parent.getParameterMap().get("com.flickr.api.key"), '@');
    if (sKey == null || sKey.isEmpty())
      return null;
    Matcher m = KEY_SEPARATOR_PATTERN.matcher(sKey);
    if (!m.find())
      throw new IllegalArgumentException("Malformed Flickr API key: " + sKey);
    return new String[]{
      sKey.substring(0, m.start()), sKey.substring(m.end()) };
  }


  private static CachingHttpClientBuilder getCachingHttpClientBuilder(
    Kaleidoscope parent )
  {
    Map<String, String> parameters = parent.getParameterMap();
    String cacheParamBase = parent.getClass().getCanonicalName() + ".cache.";
    long httpCacheSize = DefaultValueParser.parseLong(
      parameters.get(cacheParamBase + "size"), -1);
    if (httpCacheSize < 0)
      httpCacheSize = DEFAULT_HTTP_CACHE_SIZE;

    if (httpCacheSize > 0)
    {
      File cacheDir = new File(parameters.getOrDefault(
        cacheParamBase + "path", parent.getClass().getCanonicalName()));
      if (!cacheDir.isAbsolute())
      {
        cacheDir =
          PlatformPaths.getCacheDir().resolve(cacheDir.getPath()).toFile();
      }

      CachingHttpClientBuilder builder = CachingHttpClientBuilder.create()
        .setCacheConfig(CacheConfig.custom()
          .setMaxCacheEntries(Integer.MAX_VALUE)
          .setMaxObjectSize(httpCacheSize / 2)
          .setSharedCache(false)
          .setAllow303Caching(true)
          .setHeuristicCachingEnabled(true)
          .setHeuristicCoefficient(1)
          .setHeuristicDefaultLifetime(TimeUnit.DAYS.toSeconds(28))
          .build());

      try
      {
        builder.setHttpCacheStorage(new DiskLruHttpCacheStorage(
          cacheDir, HTTP_CACHE_APP_VERSION, httpCacheSize));
      }
      catch (IOException ex)
      {
        // use default cache storage
        logThrown(logger, Level.WARNING,
          "Couldn’t set up an HTTP cache in \"{0}\"",
          ex, cacheDir);
      }

      return builder;
    }

    return null;
  }


  private static ExecutorService getExecutor( Map<String, String> parameters )
  {
    int threadPoolSize = DefaultValueParser.parseInt(
      parameters.get(
        ChromasthetiationService.class.getCanonicalName() + ".threads"),
      DEFAULT_THREAD_POOL_SIZE);
    threadPoolSize = (threadPoolSize != 0) ?
      Math.max(threadPoolSize, 1) :
      Integer.MAX_VALUE;

    return new ThreadPoolExecutor(0, threadPoolSize, 30, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(1 << 6),
      new GroupedThreadFactory("Chromasthetiation", false, true));
  }


  PropertyChromasthetiator<FlickrAsync> getChromasthetiator()
  {
    if (chromasthetiator == null)
    {
      chromasthetiator = new PropertyChromasthetiator<FlickrAsync>()
        {
          @Override
          public Object getParent()
          {
            return KaleidoscopeChromasthetiationService.this;
          }
        };

      chromasthetiator.getChromatikQuery().setNHits(10);
      chromasthetiator.setFlickrApi(flickr);
      chromasthetiator.getPreferenceAdapters()
        .forEach(PropertyPreferencesAdapter::load);
    }
    return chromasthetiator;
  }


  private void loadDefaultNeutralFilter()
  {
    neutralFilter
      .getAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
    if (neutralFilter.get() != null)
      return;

    String
      paramName =
        parent.getClass().getPackage().getName() +
          ".images.filter.neutral",
      filterStr = parent.getParameterMap().get(paramName);

    if (filterStr != null && !filterStr.isEmpty())
    {
      RGBImageFilter filter;
      try
      {
        filter =
          neutralFilter.getAspect(StringConverterAspectTag.getInstance())
            .fromString(filterStr);
      }
      catch (RuntimeException ex)
      {
        logThrown(logger, Level.WARNING,
          "Illegal image filter value; {0} ignored.", ex, paramName);
        filter = null;
      }

      if (filter != null)
        neutralFilter.set(filter);
    }
  }


  public ObjectProperty<RGBImageFilter> neutralFilterProperty()
  {
    return neutralFilter;
  }

  public void setNeutralFilter( RGBImageFilter filter )
  {
    neutralFilter.set(filter);
  }

  public RGBImageFilter getNeutralFilter()
  {
    return neutralFilter.get();
  }


  public void submit( final String text )
  {
    final BiConsumer<String, Stream<? super Photo>> deferredImageQueueCompletionCallback =
      this.imageQueueCompletionCallback;
    Consumer<Stream<? super Photo>> imageQueueCompletionCallback =
      (deferredImageQueueCompletionCallback != null) ?
        (photos) -> deferredImageQueueCompletionCallback.accept(text, photos) :
        null;

    submit(text, getChromasthetiator().toSimple(),
      null, chromasthetiationCallback, imageQueueCompletionCallback,
      parent.getLayers().size());
  }


  @Override
  public String getName()
  {
    return "Chromasthetiation service";
  }


  @Override
  public Object getParent()
  {
    return parent;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.concat(
      getChromasthetiator().getPreferenceAdapters(),
      Stream.of(neutralFilter.getAspect(
        PropertyPreferencesAdapterTag.getWritableInstance())));
  }


  private class ChromasthetiationCallback
    extends AbstractFutureCallback<Pair<Image, Pair<ChromatikResponse, EmotionalState>>>
  {
    private final AtomicInteger imageListIndex = new AtomicInteger(0);


    @Override
    public void completed( Pair<Image, Pair<ChromatikResponse, EmotionalState>> response )
    {
      Image image = response.getLeft();
      assert image.getWidth(null) > 0 && image.getHeight(null) > 0 :
        image + " has width or height ≤0";
      PImage pImage = PImages.from(image);

      EmotionalState emoState = response.getRight().getRight();
      if (emoState.getStrongestEmotion().getType() == Emotion.NEUTRAL)
      {
        RGBImageFilter filter = getNeutralFilter();
        if (filter != null &&
          !(filter instanceof HSBImageFilter &&
            ((HSBImageFilter) filter).isNeutral()))
        {
          pImage =
            PImages.filter(pImage, pImage, (RGBImageFilter) filter.clone());
        }
      }

      LayerManager layers = parent.getLayers();
      int idx = imageListIndex.getAndIncrement() % layers.size();
      layers.get(idx).setNextImage(pImage);
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
          logger.log(Level.FINE,
            "Chromasthetiation failed (Flickr says: {0})",
            ex.getLocalizedMessage());
          return;
        }
      }
      super.failed(ex);
    }
  }
}
