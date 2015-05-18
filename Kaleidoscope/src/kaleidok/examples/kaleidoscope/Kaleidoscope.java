package kaleidok.examples.kaleidoscope;

import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.examples.kaleidoscope.layer.*;
import kaleidok.examples.kaleidoscope.chromatik.Chromasthetiator;
import processing.core.PApplet;
import processing.core.PImage;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
/*
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
*/


public class Kaleidoscope extends PApplet
{
  CircularLayer[] layers;

  public PImage[] images; // array to hold 4 input images

  public PImage bgImage;

  private int bgImageIndex; // variable to keep track of the current image

  public CentreMovingShape centreLayer;

  public static final int audioBufferSize = 1 << 11;

  AudioDispatcher audioDispatcher;
  private final VolumeLevelProcessor volumeLevelProcessor = new VolumeLevelProcessor();
  private Thread audioDispatcherThread;

  final Chromasthetiator chromasthetiator = new Chromasthetiator(this);


  @Override
  public void setup()
  {
    size(1000, 1000, OPENGL); // use the OpenGL renderer
    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    smooth(4);

    setupImages();

    try {
      setupAudioDispatcher();
    } catch (LineUnavailableException ex) {
      exit();
      return;
    }

    setupLayers();

    chromasthetiator.chromatikQuery.nhits = 1;
    chromasthetiator.setup();

    audioDispatcherThread.start();
  }

  private void setupImages()
  {
    // load the images from the _Images folder (relative path from this kaleidoscope's folder)
    images = new PImage[]{
      loadImage("images/cyclone.jpg"),
      loadImage("images/radar.jpg"),
      loadImage("images/happiness_texture.png"),
      loadImage("images/topo.jpg"),
      loadImage("images/particles.jpg")
    };

    bgImageIndex = (int) random(images.length); // randomly choose the bgImageIndex
    bgImage = images[bgImageIndex];
  }

  private void setupAudioDispatcher() throws LineUnavailableException
  {
    audioDispatcher = new AudioDispatcher(getLine(1, 22050, 16, audioBufferSize), audioBufferSize, 0);
    audioDispatcher.addAudioProcessor(volumeLevelProcessor);
    audioDispatcherThread = new Thread(audioDispatcher, "Audio dispatching");
  }

  private void setupLayers()
  {
    layers = new CircularLayer[]{
      new SpectrogramLayer(this, images[0], 512, 125, 290, audioDispatcher),
      new OuterMovingShape(this, images[4], 16, 300, volumeLevelProcessor),
      new FoobarLayer(this, images[3], 16, 125, 275),
      centreLayer = new CentreMovingShape(this, null, 16, 150, volumeLevelProcessor),
      null
    };
  }


  @Override
  public void draw()
  {
    drawBackgroundTexture();
    for (CircularLayer l : layers) {
      if (l != null)
        l.run();
    }
  }

  private void drawBackgroundTexture()
  {
    // background image
    image(bgImage, 0, 0, width, (float) width / height * bgImage.height); // resize-display image correctly to cover the whole screen
    fill(255, 125 + sin(frameCount * 0.01f) * 5); // white fill with dynamic transparency
    rect(0, 0, width, height); // rect covering the whole canvas
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


  public static TarsosDSPAudioInputStream getLine( int channels, int sampleRate, int sampleSize, int bufferSize )
    throws LineUnavailableException
  {
    return getLine(new AudioFormat(sampleRate, sampleSize, channels, true, true), bufferSize);
  }

  public static TarsosDSPAudioInputStream getLine( AudioFormat format, int bufferSize )
    throws LineUnavailableException
  {
    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(
      new DataLine.Info(TargetDataLine.class, format, bufferSize));
    line.open(format, bufferSize);
    line.start();
    return new JVMAudioInputStream(new AudioInputStream(line));
  }


  public static void main(String... args)
  {
    new Kaleidoscope().runSketch(args);
  }

}