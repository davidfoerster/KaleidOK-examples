package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.getflourish.stt2.SttResponse;
import com.getflourish.stt2.STT;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.chromatik.ChromasthetiationService;
import kaleidok.chromatik.ChromasthetiatorBase;
import kaleidok.chromatik.DocumentChromasthetiator;
import kaleidok.concurrent.AbstractFutureCallback;
import kaleidok.concurrent.GroupedThreadFactory;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.flickr.Flickr;
import kaleidok.flickr.FlickrException;
import kaleidok.http.cache.DiskLruHttpCacheStorage;
import kaleidok.http.cache.ExecutorSchedulingStrategy;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import kaleidok.util.DefaultValueParser;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import processing.core.PImage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JApplet;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.DebugManager.wireframe;
import static kaleidok.util.DebugManager.verbose;
import static kaleidok.util.Math.isPowerOfTwo;


public class Kaleidoscope extends ExtPApplet
{
  private CircularLayer[] layers;

  public List<PImageFuture> images; // list to hold input images

  public volatile PImageFuture bgImage;

  private int bgImageIndex; // variable to keep track of the current image

  public SpectrogramLayer spectrogramLayer;
  public OuterMovingShape outerMovingShape;
  public FoobarLayer foobarLayer;
  public CentreMovingShape centreLayer;

  public static final int DEFAULT_AUDIO_SAMPLERATE = 32000;
  public static final int DEFAULT_AUDIO_BUFFERSIZE = 1 << 11;

  private int audioBufferSize = 0;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;

  private DocumentChromasthetiator chromasthetiator;
  private ChromasthetiationService chromasthetiationService;

  private STT stt;


  public Kaleidoscope( JApplet parent )
  {
    super(parent);
  }

  @Override
  public void setup()
  {
    int width = this.width, height = this.height;
    if (width == 100 && height == 100) {
      /*
       * Default dimensions mean, the surrounding layout manager didn't resize
       * this sketch yet; use more sensible default dimensions instead (and set
       * them thereafter).
       */
      width = 1000;
      height = 1000;
    }
    size(width, height, OPENGL); // keep size, but use the OpenGL renderer
    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    smooth(4);

    getImages();
    getChromasthetiator();
    getChromasthetiationService();
    getLayers();
    getSTT();
    audioDispatcherThread.start();
  }

  private static final int MIN_IMAGES = 5;
  private List<PImageFuture> getImages()
  {
    if (images == null)
    {
      String imagesParam = getParameter(
        this.getClass().getPackage().getName() + ".images.initial");
      if (imagesParam == null) {
        this.images = new ArrayList<>(MIN_IMAGES);
      } else {
        String[] images = imagesParam.split("\\s+");
        this.images = new ArrayList<>(Math.max(images.length, MIN_IMAGES));
        for (String strImage: images) {
          if (!strImage.isEmpty()) {
            if (strImage.charAt(0) != '/' && strImage.indexOf(':') < 0)
              strImage = "/images/" + strImage;
            PImageFuture image = getImageFuture(strImage);
            if (image != null) {
              this.images.add(image);
            } else if (debug >= 1) {
              throw new AssertionError(new FileNotFoundException(strImage));
            } else {
              System.err.println("Couldn't load image: " + strImage);
            }
          }
        }
      }

      switch (images.size()) {
      case 0:
        images.add(PImageFuture.EMPTY);
        // fall through

      case 1:
        bgImageIndex = 0;
        break;

      default:
        bgImageIndex = (int) random(images.size()); // randomly choose the bgImageIndex
        break;
      }
      bgImage = images.get(bgImageIndex);
      int imageCount = images.size();
      for (int i = images.size(); i < MIN_IMAGES; i++)
        images.add(images.get(i % imageCount));
    }
    return images;
  }

  private void waitForImages()
  {
    for (PImageFuture futureImg: getImages()) {
      try {
        PImage img;
        while (true) {
          try {
            img = futureImg.get();
            break;
          } catch (InterruptedException ex) {
            // go on...
          }
        }
        assert img != null && img.width > 0 && img.height > 0;
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex.getCause());
      }
    }
  }

  private STT getSTT()
  {
    if (stt == null) {
      STT.debug = verbose >= 1;
      stt = new STT(new SttResponseHandler(),
        parseStringOrFile(getParameter("com.google.developer.api.key"), '@'));
      stt.setLanguage((String) getParameter(
        STT.class.getCanonicalName() + ".language", "en"));
      getAudioDispatcher().addAudioProcessor(stt.getAudioProcessor());
    }
    return stt;
  }

  private AudioDispatcher getAudioDispatcher()
  {
    if (audioDispatcher == null) {
      String paramBase = this.getClass().getPackage().getName() + ".audio.";
      String audioSource = getParameter(paramBase + "input");

      String param =  paramBase + "samplerate";
      int sampleRate = DefaultValueParser.parseInt(this,
        param, DEFAULT_AUDIO_SAMPLERATE);
      if (sampleRate <= 0)
        throw new AssertionError(param + " must be positive");

      int bufferSize = getAudioBufferSize();

      param = paramBase + "bufferoverlap";
      int bufferOverlap = DefaultValueParser.parseInt(this,
       param, bufferSize / 2);
      if (bufferOverlap < 0 || bufferOverlap >= bufferSize)
        throw new AssertionError(param + " must be positive and less than buffersize");
      if (bufferOverlap > 0 && !isPowerOfTwo(bufferOverlap))
        throw new AssertionError(param + " must be a power of 2");

      Runnable dispatcherRunnable;

      try {
        if (audioSource == null) {
          audioDispatcher =
            fromDefaultMicrophone(sampleRate, bufferSize, bufferOverlap);
          dispatcherRunnable = audioDispatcher;
        } else {
          InputStream is = createInputRaw(audioSource);
          if (is == null)
            throw new FileNotFoundException(audioSource);
          if (!is.markSupported())
            is = new ByteArrayInputStream(loadBytes(is));
          AudioInputStream ais = getAudioInputStream(is);
          audioDispatcher =
            new AudioDispatcher(new ContinuousAudioInputStream(ais),
              bufferSize, bufferOverlap);

          boolean play =
            DefaultValueParser.parseBoolean(this, paramBase + "input.play", false);
          if (play) {
            dispatcherRunnable = audioDispatcher;
            audioDispatcher.addAudioProcessor(new AudioPlayer(
              JVMAudioInputStream.toAudioFormat(audioDispatcher.getFormat()),
              bufferSize));
          } else {
            dispatcherRunnable =
              new DummyAudioPlayer().addToDispatcher(audioDispatcher);
          }
        }
      } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
        throw new Error(ex);
      }

      audioDispatcher.addAudioProcessor(getVolumeLevelProcessor());
      audioDispatcher.addAudioProcessor(getFftProcessor());

      makeAudioDispatcherThread(dispatcherRunnable);
    }
    return audioDispatcher;
  }


  public int getAudioBufferSize()
  {
    if (audioBufferSize <= 0) {
      String param =
        this.getClass().getPackage().getName() + ".audio.buffersize";
      int bufferSize = DefaultValueParser.parseInt(this, param,
        DEFAULT_AUDIO_BUFFERSIZE);
      if (bufferSize <= 0 || !isPowerOfTwo(bufferSize))
        throw new AssertionError(param + " must be a power of 2");
      audioBufferSize = bufferSize;
    }
    return audioBufferSize;
  }


  private VolumeLevelProcessor getVolumeLevelProcessor()
  {
    if (volumeLevelProcessor == null)
      volumeLevelProcessor = new VolumeLevelProcessor();
    return volumeLevelProcessor;
  }


  private MinimFFTProcessor getFftProcessor()
  {
    if (fftProcessor == null)
      fftProcessor = new MinimFFTProcessor(getAudioBufferSize());
    return fftProcessor;
  }


  private void makeAudioDispatcherThread( final Runnable dispatcher )
  {
    if (audioDispatcherThread == null) {
      audioDispatcherThread = new Thread(new Runnable()
        {
          @Override
          public void run()
          {
            waitForImages();
            dispatcher.run();
          }
        },
        "Audio dispatching");
      audioDispatcherThread.setDaemon(true);
    }
  }

  private CircularLayer[] getLayers()
  {
    if (layers == null)
    {
      getAudioDispatcher();

      layers = new CircularLayer[]{
        spectrogramLayer =
          new SpectrogramLayer(this, images.get(0), 256, 125, 290,
            getFftProcessor()),
        getOuterMovingShape(),
        foobarLayer =
          new FoobarLayer(this, images.get(3), 16, 125, 275),
        centreLayer =
          new CentreMovingShape(this, null, 16, 150, getVolumeLevelProcessor())
      };
    }
    return layers;
  }

  private OuterMovingShape getOuterMovingShape()
  {
    if (outerMovingShape == null) {
      OuterMovingShape outerMovingShape =
        new OuterMovingShape(this, getImages().get(4), 16, 300);
      AudioDispatcher audioDispatcher = getAudioDispatcher();
      audioDispatcher.addAudioProcessor(new PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(), audioBufferSize,
        outerMovingShape.getPitchDetectionHandler()));
      this.outerMovingShape = outerMovingShape;
    }
    return outerMovingShape;
  }


  private DocumentChromasthetiator getChromasthetiator()
  {
    if (chromasthetiator == null) {
      ChromasthetiatorBase.verbose = verbose;
      chromasthetiator = new DocumentChromasthetiator(this);

      String data = parseStringOrFile(getParameter("com.flickr.api.key"), '@');
      if (data != null) {
        String[] keys = data.split(":|\r?\n", 2);
        chromasthetiator.setFlickrApi(new Flickr(keys[0], keys[1]));
      }

      chromasthetiator.maxKeywords = DefaultValueParser.parseInt(this,
        chromasthetiator.getClass().getPackage().getName() + ".maxKeywords",
        chromasthetiator.maxKeywords);

      chromasthetiator.chromatikQuery.nhits = 10;
    }
    return chromasthetiator;
  }


  public static final long DEFAULT_HTTP_CACHE_SIZE = 50L << 20;

  private static final int HTTP_CACHE_APP_VERSION = 1;

  private ChromasthetiationService getChromasthetiationService()
  {
    if (chromasthetiationService == null)
    {
      CachingHttpClientBuilder builder = CachingHttpClientBuilder.create();
      builder
        .disableConnectionState()
        .disableCookieManagement();

      int threadPoolSize = getChromasthetiationThreadPoolSize();
      ThreadFactory threadFactory =
        new GroupedThreadFactory("Chromastetiation", true);
      ExecutorService executor = (threadPoolSize == 0) ?
        Executors.newCachedThreadPool(threadFactory) :
        Executors.newFixedThreadPool(threadPoolSize, threadFactory);
      builder.setSchedulingStrategy(
        new ExecutorSchedulingStrategy(executor));

      long httpCacheSize = DEFAULT_HTTP_CACHE_SIZE;
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
          this.getClass().getCanonicalName(),
          HTTP_CACHE_APP_VERSION, httpCacheSize));
        // TODO: Add configuration property for HTTP cache size and base directory
      } catch (IOException ex) {
        if (debug >= 1)
          throw new AssertionError(ex);
        // else: use default cache storage
      }

      builder.setCacheConfig(cacheConfig);
      chromasthetiationService = new ChromasthetiationService(
        executor, Executor.newInstance(builder.build()));
    }
    return chromasthetiationService;
  }


  private int getChromasthetiationThreadPoolSize()
  {
    return DefaultValueParser.parseInt(this,
      ChromasthetiationService.class.getCanonicalName() + ".threads",
      ChromasthetiationService.DEFAULT_THREAD_POOL_SIZE);
  }


  @Override
  public void destroy()
  {
    stt.shutdown();
    super.destroy();
  }


  @Override
  public void draw()
  {
    final long start = System.nanoTime();

    drawBackgroundTexture();
    for (CircularLayer l : layers) {
      if (l != null)
        l.run();
    }

    if (verbose >= 1)
      drawFrameRate(start);
  }


  private void drawBackgroundTexture()
  {
    PImage bgImage;
    if (wireframe < 1 && (bgImage = this.bgImage.getNoThrow()) != null) {
      // background image
      image(bgImage, 0, 0, width,
        (float) width / height * bgImage.height); // resize-display image correctly to cover the whole screen
      fill(255, 125 + sin(frameCount * 0.01f) * 5); // white fill with dynamic transparency
      rect(0, 0, width, height); // rect covering the whole canvas
    } else {
      background(0);
    }
  }


  private String sampledFrameRate = "";

  private void drawFrameRate( long start )
  {
    if (frameCount % 15 == 0)
      sampledFrameRate = String.valueOf((int) frameRate);

    final int offset = 4, size = 8;
    textSize(size);
    fill(0, 255, 0);
    text(sampledFrameRate, offset, size + offset);
    text((int)(System.nanoTime() - start) / 1000000, offset, 2*size + offset);
  }


  public void chromasthetiate( String text )
  {
    getChromasthetiationService().submit(text, getChromasthetiator(),
      null, chromasthetiationCallback, getLayers().length + 1);
  }


  private final ChromasthetiationCallback chromasthetiationCallback =
    new ChromasthetiationCallback();

  private class ChromasthetiationCallback
    extends AbstractFutureCallback<Image>
  {
    private final AtomicInteger imageListIndex = new AtomicInteger(0);

    @Override
    public void completed( Image image )
    {
      assert image.getWidth(null) > 0 && image.getHeight(null) > 0;
      PImageFuture fImage = new PImageFuture(new PImage(image));

      CircularLayer[] layers = getLayers();
      int idx = imageListIndex.getAndIncrement() % (layers.length + 1) - 1;
      if (idx >= 0) {
        layers[idx].currentImage = fImage;
      } else {
        bgImage = fImage;
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


  @Override
  public void keyTyped()
  {
    switch (key) {
    case 'i':
      stt.begin(false);
      break;

    case 'o':
      stt.end(false);
      break;
    }
  }


  private class SttResponseHandler extends AbstractFutureCallback<SttResponse>
  {
    private Boolean isIgnoreTranscriptionResult = null;

    @Override
    public void completed( SttResponse response )
    {
      SttResponse.Result result = response.result[0];

      if (verbose >= 1)
        println("STT returned: " + result.alternative[0].transcript);

      if (!isIgnoreTranscriptionResult())
        chromasthetiate(result.alternative[0].transcript);
    }

    private boolean isIgnoreTranscriptionResult()
    {
      if (isIgnoreTranscriptionResult == null) {
        isIgnoreTranscriptionResult =
          DefaultValueParser.parseBoolean(Kaleidoscope.this,
            this.getClass().getPackage().getName() + ".ignoreTranscription",
            false);
        if (isIgnoreTranscriptionResult) {
          System.out.println(
            "Notice: Speech transcription results are configured to be ignored.");
        }
      }
      return isIgnoreTranscriptionResult;
    }
  }


  private String parseStringOrFile( String s, char filePrefix )
  {
    if (s != null && !s.isEmpty() && s.charAt(0) == filePrefix) {
      s = new String((s.length() == 2 && s.charAt(1) == '-') ?
          loadBytes(System.in) : loadBytes(s.substring(1)));
      String ls = System.getProperty("line.separator");
      int sLen = s.length(), lsLen = ls.length();
      while (sLen >= lsLen && s.startsWith(ls, sLen - lsLen))
        sLen -= lsLen;
      s = s.substring(0, sLen);
    }
    return s;
  }

}
