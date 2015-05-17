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
import synesketch.SynesketchState;
/*
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
*/


class Kaleidoscope extends PApplet
{
  CircularLayer[] layers;

  private PImage[] images; // array to hold 4 input images

  private int currentImage; // variable to keep track of the current image

  private int currentImage2; //for the second

  public static final int audioBufferSize = 1 << 11;

  //AudioInput audioSource;
  AudioDispatcher audioDispatcher;

  private Thread audioDispatcherThread;

  final SpeechChromasthetiator speechChromasthetiator = new SpeechChromasthetiator(this);


  @Override
  public void setup()
  {
    size(1000, 1000, OPENGL); // use the OpenGL renderer
    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    smooth(4);

    try {
      setupImages();
      //audioSource = new Minim(this).getLineIn(Minim.MONO, 2048, 22050);
      setupAudioDispatcher();
      setupLayers();
      speechChromasthetiator.setup();
      audioDispatcherThread.start();
    } catch (LineUnavailableException ex) {
      exit();
    }
  }

  private void setupImages()
  {
    // load the images from the _Images folder (relative path from this sketch's folder)
    images = new PImage[]{
      loadImage("images/cyclone.jpg"),
      loadImage("images/radar.jpg"),
      loadImage("images/happiness_texture.png"),
      loadImage("images/topo.jpg"),
      loadImage("images/particles.jpg")
    };

    currentImage = (int) random(images.length); // randomly choose the currentImage
    currentImage2 = (int) random(images.length);
  }

  private void setupAudioDispatcher() throws LineUnavailableException
  {
    audioDispatcher = new AudioDispatcher(getLine(1, 22050, 16, audioBufferSize), audioBufferSize, 0);
    audioDispatcherThread = new Thread(audioDispatcher, "Audio dispatching");
  }

  private void setupLayers()
  {
    layers = new CircularLayer[]{
      new SpectrogramLayer(this, images[0], 512, 125, 290, audioDispatcher),
      new OuterMovingShape(this, images[4], 16, 300),
      new FoobarLayer(this, images[3], 16, 125, 275),
      new CentreMovingShape(this, images[currentImage2], 16, 150),
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
    image(images[currentImage], 0, 0, width, (float) width/height*images[currentImage].height); // resize-display image correctly to cover the whole screen
    fill(255, 125+sin(frameCount*0.01f)*5); // white fill with dynamic transparency
    rect(0, 0, width, height); // rect covering the whole canvas
  }


  @Override
  public void keyPressed()
  {
    //currentImage = (currentImage + 1) % images.length;
    speechChromasthetiator.keyPressed();
  }

  @Override
  public void keyReleased()
  {
    speechChromasthetiator.keyReleased();
  }

  public void transcribe( String utterance, float confidence )
  {
    speechChromasthetiator.transcribe(utterance, confidence);
  }

  public void synesketchUpdate( SynesketchState state )
  {
    speechChromasthetiator.synesketchUpdate(state);
  }

  @Override
  public void mouseClicked()
  {
    currentImage2 = (currentImage2 + 1) % images.length;
    layers[3].currentImage = images[currentImage2];
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
