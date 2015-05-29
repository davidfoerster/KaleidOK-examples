package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
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
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.List;

import static kaleidok.util.DebugManager.wireframe;
import static kaleidok.util.DebugManager.verbose;


public class Kaleidoscope extends PApplet implements Chromasthetiator.SearchResultHandler, TranscriptionResultHandler
{
  private CircularLayer[] layers;

  public PImage[] images; // array to hold 4 input images

  public PImage bgImage;

  private int bgImageIndex; // variable to keep track of the current image

  public SpectrogramLayer spectrogramLayer;
  public OuterMovingShape outerMovingShape;
  public FoobarLayer foobarLayer;
  public CentreMovingShape centreLayer;

  public static final int audioBufferSize = 1 << 11;
  public static final int audioOverlap = audioBufferSize / 2;
  public static final int audioSampleRate = 22050;

  private final String audioSource;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;

  final Chromasthetiator chromasthetiator = new Chromasthetiator(this, this);

  private STT stt;


  public Kaleidoscope()
  {
    this(null);
  }

  public Kaleidoscope( String audioSource )
  {
    super();
    this.audioSource = audioSource;
  }

  @Override
  public void setup()
  {
    size(1000, 1000, OPENGL); // use the OpenGL renderer
    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    smooth(4);

    try {
      setupAudioDispatcher();
    } catch (LineUnavailableException | UnsupportedAudioFileException | IOException ex) {
      ex.printStackTrace();
      exit();
      return;
    }

    setupImages();
    setupLayers();
    setupChromasthetiator();

    stt = new STT(this, null);
    stt.setDebug(true);
    stt.setLanguage("en");
    audioDispatcher.addAudioProcessor(stt);

    audioDispatcherThread.start();
  }

  private void setupImages()
  {
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

  public PImage loadImage( String path, boolean throwOnFailure )
    throws RuntimeException
  {
    PImage img = super.loadImage(path);
    if (!throwOnFailure || (img != null && img.width > 0 && img.height > 0))
      return img;
    throw new RuntimeException("Couldn't load image: " + path);
  }

  private void setupAudioDispatcher()
    throws LineUnavailableException, IOException, UnsupportedAudioFileException
  {
    Runnable dispatcherRunnable;

    if (audioSource == null) {
      audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
        audioSampleRate, audioBufferSize, audioOverlap);
      dispatcherRunnable = audioDispatcher;
    } else {
      AudioInputStream ais =
        AudioSystem.getAudioInputStream(createInputRaw(audioSource));
      audioDispatcher =
        new AudioDispatcher(new ContinuousAudioInputStream(ais),
          audioBufferSize, audioOverlap);
      dispatcherRunnable =
        new DummyAudioPlayer().addToDispatcher(audioDispatcher);
    }

    audioDispatcher.addAudioProcessor(
      volumeLevelProcessor = new VolumeLevelProcessor());
    audioDispatcher.addAudioProcessor(
      fftProcessor = new MinimFFTProcessor(audioBufferSize));

    audioDispatcherThread =
      new Thread(dispatcherRunnable, "Audio dispatching");
  }

  private void setupLayers()
  {
    layers = new CircularLayer[]{
      spectrogramLayer =
        new SpectrogramLayer(this, images[0], 256, 125, 290, fftProcessor),
      outerMovingShape =
        new OuterMovingShape(this, images[4], 16, 300, audioDispatcher),
      foobarLayer =
        new FoobarLayer(this, images[3], 16, 125, 275),
      centreLayer =
        new CentreMovingShape(this, null, 16, 150, volumeLevelProcessor),
      null
    };
  }

  private void setupChromasthetiator()
  {
    String[] keys = loadStrings("api-key.flickr.txt");
    Flickr flickr = new Flickr(keys[0], keys[1], new REST());
    chromasthetiator.setFlickrApi(flickr);

    //chromasthetiator.chromatikQuery.nhits = 1;
    chromasthetiator.setup();
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
  public void keyPressed()
  {
    stt.begin();
  }

  @Override
  public void keyReleased()
  {
    stt.end();
  }

  @Override
  public void handleTranscriptionResult( Response.Result result )
  {
    println("STT result: " + result.alternative[0].transcript);
  }
}
