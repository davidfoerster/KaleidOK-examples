package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.util.DefaultValueParser;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JApplet;
import java.io.*;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static kaleidok.util.DebugManager.verbose;
import static kaleidok.util.Math.isPowerOfTwo;


public class Kaleidoscope extends ExtPApplet
{
  private LayerManager layers;

  public static final int DEFAULT_AUDIO_SAMPLERATE = 32000;
  public static final int DEFAULT_AUDIO_BUFFERSIZE = 1 << 11;

  private int audioBufferSize = 0;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;

  private KaleidoscopeChromasthetiationService chromasthetiationService;

  private SttManager stt;


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
    previousWidth = width;
    previousHeight = height;

    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)

    int smoothingLevel = DefaultValueParser.parseInt(this,
      g.getClass().getCanonicalName() + ".smooth", 4);
    if (smoothingLevel > 0) {
      smooth(smoothingLevel);
    } else {
      noSmooth();
    }

    getLayers();
    getChromasthetiationService();
    getSTT();

    audioDispatcherThread.start();

    if (verbose >= 1)
      new FrameRateDisplay(this);
  }


  public LayerManager getLayers()
  {
    if (layers == null)
      layers = new LayerManager(this);
    return layers;
  }


  private SttManager getSTT()
  {
    if (stt == null)
      stt = new SttManager(this);
    return stt;
  }


  AudioDispatcher getAudioDispatcher()
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


  VolumeLevelProcessor getVolumeLevelProcessor()
  {
    if (volumeLevelProcessor == null)
      volumeLevelProcessor = new VolumeLevelProcessor();
    return volumeLevelProcessor;
  }


  MinimFFTProcessor getFftProcessor()
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
            getLayers().waitForImages();
            dispatcher.run();
          }
        },
        "Audio dispatching");
      audioDispatcherThread.setDaemon(true);
    }
  }


  public KaleidoscopeChromasthetiationService getChromasthetiationService()
  {
    if (chromasthetiationService == null)
      chromasthetiationService = KaleidoscopeChromasthetiationService.newInstance(this);
    return chromasthetiationService;
  }


  @Override
  public void destroy()
  {
    stt.shutdown();
    super.destroy();
  }


  private int previousWidth = -1, previousHeight = -1;

  @Override
  public void draw()
  {
    getLayers().draw();
    previousWidth = width;
    previousHeight = height;
  }


  public boolean wasResized()
  {
    return width != previousWidth || height != previousHeight;
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


  String parseStringOrFile( String s, char filePrefix )
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
