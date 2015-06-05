package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.getflourish.stt2.Response;
import com.getflourish.stt2.STT;
import com.getflourish.stt2.TranscriptionResultHandler;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.examples.kaleidoscope.chromatik.Chromasthetiator;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.processing.ExtPApplet;
import kaleidok.util.DefaultValueParser;
import processing.core.PImage;
import processing.data.JSONArray;

import javax.sound.sampled.*;
import javax.swing.JApplet;
import java.io.IOException;
import java.util.List;

import static kaleidok.util.DebugManager.wireframe;
import static kaleidok.util.DebugManager.verbose;
import static kaleidok.util.Math.isPowerOfTwo;


public class Kaleidoscope extends ExtPApplet
  implements Chromasthetiator.SearchResultHandler, TranscriptionResultHandler
{
  private CircularLayer[] layers;

  public PImage[] images; // array to hold 4 input images

  public PImage bgImage;

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

    try {
      makeAudioDispatcher();
      getImages();
      getLayers();
      getChromasthetiator();
      getSTT();
      makeAudioDispatcherThread().start();
    }
    catch (LineUnavailableException | UnsupportedAudioFileException | IOException ex) {
      ex.printStackTrace();
      exit();
    }
  }

  private PImage[] getImages()
  {
    if (images == null) {
      // load the images from the _Images folder (relative path from this kaleidoscope's folder)
      images = new PImage[]{
        loadImage("images/one.jpg", true),
        loadImage("images/two.jpg", true),
        loadImage("images/three.jpg", true),
        loadImage("images/four.jpg", true),
        loadImage("images/five.jpg", true)
      };

      bgImageIndex = (int) random(images.length); // randomly choose the bgImageIndex
      bgImage = images[bgImageIndex];
    }
    return images;
  }

  public PImage loadImage( String path, boolean throwOnFailure )
    throws RuntimeException
  {
    PImage img = super.loadImage(path);
    if (!throwOnFailure || (img != null && img.width > 0 && img.height > 0))
      return img;
    throw new RuntimeException("Couldn't load image: " + path);
  }

  private STT getSTT() throws IOException
  {
    if (stt == null) {
      stt = new STT(this,
        parseStringOrFile(getParameter("com.google.developer.api.key"), '@'));
      stt.setDebug(verbose > 0);
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

      int sampleRate = DefaultValueParser.parseInt(this,
        paramBase + "samplerate", DEFAULT_AUDIO_SAMPLERATE);
      if (sampleRate <= 0)
        throw new AssertionError(paramBase + "samplerate" + " must be positive");

      int bufferSize = DefaultValueParser.parseInt(this,
        paramBase + "buffersize", DEFAULT_AUDIO_BUFFERSIZE);
      if (bufferSize <= 0 || !isPowerOfTwo(bufferSize))
        throw new AssertionError(paramBase + "buffersize" + " must be a power of 2");
      audioBufferSize = bufferSize;

      int bufferOverlap = DefaultValueParser.parseInt(this,
       paramBase + "bufferoverlap", bufferSize / 2);
      if (bufferOverlap < 0 || bufferOverlap >= bufferSize)
        throw new AssertionError(paramBase + "bufferoverlap" + " must be positive and less than buffersize");
      if (bufferOverlap > 0 && !isPowerOfTwo(bufferSize))
        throw new AssertionError(paramBase + "bufferoverlap" + " must be a power of 2");

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

      audioDispatcherThread =
        new Thread(dispatcherRunnable, "Audio dispatching");
    }
    return audioDispatcher;
  }

  private Thread makeAudioDispatcherThread()
    throws UnsupportedAudioFileException, IOException, LineUnavailableException
  {
    makeAudioDispatcher();
    return audioDispatcherThread;
  }

  private CircularLayer[] getLayers()
  {
    if (layers == null)
    {
      outerMovingShape = new OuterMovingShape(this, images[4], 16, 300);
      audioDispatcher.addAudioProcessor(new PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
        audioDispatcher.getFormat().getSampleRate(), audioBufferSize,
        outerMovingShape.getPitchDetectionHandler()));

      layers = new CircularLayer[]{
        spectrogramLayer =
          new SpectrogramLayer(this, images[0], 256, 125, 290, fftProcessor),
        outerMovingShape,
        foobarLayer =
          new FoobarLayer(this, images[3], 16, 125, 275),
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
    if (wireframe < 1) {
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
      bgImageIndex = (bgImageIndex + 1) % images.length;
      bgImage = images[bgImageIndex];
      break;
    }
  }

  @Override
  public void handleChromatikResult( JSONArray queryResult, List<PImage> resultSet )
  {
    if (!resultSet.isEmpty()) {
      int i = 0, j = 0;
      bgImage = resultSet.get(j++);
      while (i < layers.length && j < resultSet.size()) {
        CircularLayer layer = layers[i++];
        if (layer != null)
          layer.currentImage = resultSet.get(j++);
      }
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


  @Override
  public void handleTranscriptionResult( Response.Result result )
  {
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
