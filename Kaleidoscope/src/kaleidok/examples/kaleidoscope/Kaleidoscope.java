package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.getflourish.stt2.SttResponse;
import com.getflourish.stt2.STT;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.concurrent.Callback;
import kaleidok.examples.kaleidoscope.chromatik.Chromasthetiator;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.PImageFuture;
import kaleidok.util.DefaultValueParser;
import processing.core.PImage;
import processing.data.JSONArray;

import javax.sound.sampled.*;
import javax.swing.JApplet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static kaleidok.util.DebugManager.wireframe;
import static kaleidok.util.DebugManager.verbose;
import static kaleidok.util.Math.isPowerOfTwo;


public class Kaleidoscope extends ExtPApplet
  implements Chromasthetiator.SearchResultHandler
{
  private CircularLayer[] layers;

  public List<PImageFuture> images; // list to hold input images

  public PImageFuture bgImage;

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

  private Chromasthetiator chromasthetiator;

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
    AudioDispatcher audioDispatcher;
    try {
      getChromasthetiator();
      audioDispatcher = makeAudioDispatcher();
      makeSTT(audioDispatcher);
    }
    catch (LineUnavailableException | UnsupportedAudioFileException | IOException ex) {
      ex.printStackTrace();
      exit();
      return;
    }
    getLayers(audioDispatcher, fftProcessor, volumeLevelProcessor);
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

  private STT makeSTT( AudioDispatcher audioDispatcher ) throws IOException
  {
    if (stt == null) {
      STT.debug = verbose >= 1;
      stt = new STT(new SttResponseHandler(),
        parseStringOrFile(getParameter("com.google.developer.api.key"), '@'));
      stt.setLanguage((String) getParameter(
        STT.class.getCanonicalName() + ".language", "en"));
      audioDispatcher.addAudioProcessor(stt.getAudioProcessor());
    }
    return stt;
  }

  private AudioDispatcher makeAudioDispatcher()
    throws LineUnavailableException, IOException, UnsupportedAudioFileException
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
      if (bufferOverlap > 0 && !isPowerOfTwo(bufferSize))
        throw new AssertionError(param + " must be a power of 2");

      Runnable dispatcherRunnable;

      if (audioSource == null) {
        audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
          sampleRate, bufferSize, bufferOverlap);
        dispatcherRunnable = audioDispatcher;
      } else {
        AudioInputStream ais =
          AudioSystem.getAudioInputStream(createInputRaw(audioSource));
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

      audioDispatcher.addAudioProcessor(
        volumeLevelProcessor = new VolumeLevelProcessor());
      audioDispatcher.addAudioProcessor(
        fftProcessor = new MinimFFTProcessor(bufferSize));

      makeAudioDispatcherThread(dispatcherRunnable);
    }
    return audioDispatcher;
  }

  private Thread makeAudioDispatcherThread( final Runnable dispatcher )
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
    return audioDispatcherThread;
  }

  private CircularLayer[] getLayers( AudioDispatcher audioDispatcher,
    MinimFFTProcessor fftProcessor, VolumeLevelProcessor volumeLevelProcessor )
  {
    if (layers == null)
    {
      outerMovingShape = new OuterMovingShape(this, images.get(4), 16, 300);
      audioDispatcher.addAudioProcessor(new PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(), audioBufferSize,
        outerMovingShape.getPitchDetectionHandler()));

      layers = new CircularLayer[]{
        spectrogramLayer =
          new SpectrogramLayer(this, images.get(0), 256, 125, 290, fftProcessor),
        outerMovingShape,
        foobarLayer =
          new FoobarLayer(this, images.get(3), 16, 125, 275),
        centreLayer =
          new CentreMovingShape(this, null, 16, 150, volumeLevelProcessor),
        null
      };
    }
    return layers;
  }

  Chromasthetiator getChromasthetiator() throws IOException
  {
    if (chromasthetiator == null) {
      chromasthetiator = new Chromasthetiator(this, this);

      String data = parseStringOrFile(getParameter("com.flickr.api.key"), '@');
      if (data != null) {
        String[] keys = data.split(":|\r?\n", 2);
        chromasthetiator.setFlickrApi(
          new Flickr(keys[0], keys[1], new REST()));
      }

      chromasthetiator.maxKeywords = DefaultValueParser.parseInt(this,
        chromasthetiator.getClass().getPackage().getName() + ".maxKeywords",
        chromasthetiator.maxKeywords);

      //chromasthetiator.chromatikQuery.nhits = 1;

      chromasthetiator.setup();
    }
    return chromasthetiator;
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

  @Override
  public void handleChromatikResult( JSONArray queryResult, List<PImage> resultSet )
  {
    // TODO: Adapt to PImageFuture
    /*
    if (!resultSet.isEmpty()) {
      int i = 0, j = 0;
      bgImage = resultSet.get(j++);
      while (i < layers.length && j < resultSet.size()) {
        CircularLayer layer = layers[i++];
        if (layer != null)
          layer.currentImage = resultSet.get(j++);
      }
    }
    */
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


  private class SttResponseHandler implements Callback<SttResponse>
  {
    @Override
    public void call( SttResponse response )
    {
      SttResponse.Result result = response.result[0];

      if (verbose >= 1)
        println("STT returned: " + result.alternative[0].transcript);

      if (!isIgnoreTranscriptionResult()) {
        try {
          getChromasthetiator().issueQuery(result.alternative[0].transcript);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private boolean isIgnoreTranscriptionResult()
  {
    if (isIgnoreTranscriptionResult == null) {
      isIgnoreTranscriptionResult = DefaultValueParser.parseBoolean(this,
        this.getClass().getPackage().getName() + ".ignoreTranscription",
        false);
      if (isIgnoreTranscriptionResult) {
        System.out.println(
          "Notice: Speech transcription results are configured to be ignored.");
      }
    }
    return isIgnoreTranscriptionResult;
  }

  private Boolean isIgnoreTranscriptionResult = null;


  private String parseStringOrFile( String s, char filePrefix )
    throws IOException
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
