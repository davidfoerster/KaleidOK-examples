package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
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
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import kaleidok.util.DefaultValueParser;
import processing.core.PImage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JApplet;
import java.awt.Image;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;
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

  private int audioBufferSize;
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
    size(1000, 1000, OPENGL); // use the OpenGL renderer
    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    smooth(4);

    getImages();
    getChromasthetiator();
    getChromasthetiationService();
    getLayers();
    getSTT();
    audioDispatcherThread.start();
  }

  private List<PImageFuture> getImages()
  {
    if (images == null) {
      // load the images from the _Images folder (relative path from this kaleidoscope's folder)
      images = new ArrayList<PImageFuture>(8) {{
        add(getImageFuture("images/one.jpg"));
        add(getImageFuture("images/two.jpg"));
        add(getImageFuture("images/three.jpg"));
        add(getImageFuture("images/four.jpg"));
        add(getImageFuture("images/five.jpg"));
      }};

      bgImageIndex = (int) random(images.size()); // randomly choose the bgImageIndex
      bgImage = images.get(bgImageIndex);
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

      param = paramBase + "buffersize";
      int bufferSize = DefaultValueParser.parseInt(this,
        param, DEFAULT_AUDIO_BUFFERSIZE);
      if (bufferSize <= 0 || !isPowerOfTwo(bufferSize))
        throw new AssertionError(param + " must be a power of 2");
      audioBufferSize = bufferSize;

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

      audioDispatcher.addAudioProcessor(
        volumeLevelProcessor = new VolumeLevelProcessor());
      audioDispatcher.addAudioProcessor(
        fftProcessor = new MinimFFTProcessor(bufferSize));

      makeAudioDispatcherThread(dispatcherRunnable);
    }
    return audioDispatcher;
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
    }
  }

  private CircularLayer[] getLayers()
  {
    if (layers == null)
    {
      getAudioDispatcher();

      layers = new CircularLayer[]{
        spectrogramLayer =
          new SpectrogramLayer(this, images.get(0), 256, 125, 290, fftProcessor),
        getOuterMovingShape(),
        foobarLayer =
          new FoobarLayer(this, images.get(3), 16, 125, 275),
        centreLayer =
          new CentreMovingShape(this, null, 16, 150, volumeLevelProcessor)
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
        chromasthetiator.setFlickrApi(
          new Flickr(keys[0], keys[1], new REST()));
      }

      chromasthetiator.maxKeywords = DefaultValueParser.parseInt(this,
        chromasthetiator.getClass().getPackage().getName() + ".maxKeywords",
        chromasthetiator.maxKeywords);

      chromasthetiator.chromatikQuery.nhits = 10;
    }
    return chromasthetiator;
  }


  private ChromasthetiationService getChromasthetiationService()
  {
    if (chromasthetiationService == null) {
      chromasthetiationService =
        new ChromasthetiationService(getChromasthetiationThreadPoolSize());
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


  @Override
  public void mouseClicked()
  {
    switch (mouseButton) {
    case LEFT:
      if (centreLayer != null)
        centreLayer.nextImage();
      break;

    case RIGHT:
      bgImageIndex = (bgImageIndex + 1) % images.size();
      bgImage = images.get(bgImageIndex);
      break;
    }
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
        switch (Integer.parseInt(((FlickrException) ex).getErrorCode())) {
        case 1: // Photo not found
        case 2: // Permission denied
          //noinspection unchecked
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
