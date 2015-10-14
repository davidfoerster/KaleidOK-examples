package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.Math.isPowerOfTwo;


public class AudioProcessingManager
{
  public static int DEFAULT_AUDIO_SAMPLERATE = 32000;
  public static int DEFAULT_AUDIO_BUFFERSIZE = 1 << 11;

  private final Kaleidoscope parent;

  private int audioBufferSize = 0;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;


  AudioProcessingManager( Kaleidoscope parent )
  {
    this.parent = parent;
  }


  AudioDispatcher getAudioDispatcher()
  {
    initAudioDispatcher();
    return audioDispatcher;
  }


  Thread getAudioDispatcherThread()
  {
    initAudioDispatcher();
    return audioDispatcherThread;
  }


  private void initAudioDispatcher()
  {
    if (audioDispatcher == null)
    {
      String paramBase = parent.getClass().getPackage().getName() + ".audio.";
      String audioSource = parent.getParameter(paramBase + "input");

      String param =  paramBase + "samplerate";
      int sampleRate = DefaultValueParser.parseInt(parent,
        param, DEFAULT_AUDIO_SAMPLERATE);
      if (sampleRate <= 0)
        throw new AssertionError(param + " must be positive");

      int bufferSize = getAudioBufferSize();

      param = paramBase + "bufferoverlap";
      int bufferOverlap = DefaultValueParser.parseInt(parent,
        param, bufferSize / 2);
      if (bufferOverlap < 0 || bufferOverlap >= bufferSize)
        throw new AssertionError(param + " must be positive and less than buffersize");
      if (bufferOverlap > 0 && !isPowerOfTwo(bufferOverlap))
        throw new AssertionError(param + " must be a power of 2");

      Runnable dispatcherRunnable;

      try {
        if (audioSource == null)
        {
          audioDispatcher =
            fromDefaultMicrophone(sampleRate, bufferSize, bufferOverlap);
          dispatcherRunnable = audioDispatcher;
        }
        else
        {
          InputStream is = parent.createInputRaw(audioSource);
          if (is == null)
            throw new FileNotFoundException(audioSource);
          if (!is.markSupported())
            is = new ByteArrayInputStream(PApplet.loadBytes(is));
          AudioInputStream ais = getAudioInputStream(is);
          audioDispatcher =
            new AudioDispatcher(new ContinuousAudioInputStream(ais),
              bufferSize, bufferOverlap);

          logger.config(audioDispatcher.getFormat().toString());

          boolean play =
            DefaultValueParser.parseBoolean(parent, paramBase + "input.play", false);
          if (play)
          {
            dispatcherRunnable = audioDispatcher;
            audioDispatcher.addAudioProcessor(new AudioPlayer(
              JVMAudioInputStream.toAudioFormat(audioDispatcher.getFormat()),
              bufferSize));
          }
          else
          {
            dispatcherRunnable =
              new DummyAudioPlayer().addToDispatcher(audioDispatcher);
          }
        }
      } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
        throw new Error(ex);
      }

      audioDispatcher.addAudioProcessor(getVolumeLevelProcessor());
      audioDispatcher.addAudioProcessor(getFftProcessor());

      initAudioDispatcherThread(dispatcherRunnable);
    }
  }


  private void initAudioDispatcherThread( final Runnable dispatcher )
  {
    if (audioDispatcherThread == null)
    {
      audioDispatcherThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          parent.getLayers().waitForImages();
          dispatcher.run();
        }
      },
        "Audio dispatching");
      audioDispatcherThread.setDaemon(true);
      audioDispatcherThread.setPriority(Thread.NORM_PRIORITY + 1);
    }
  }


  public int getAudioBufferSize()
  {
    if (audioBufferSize <= 0)
    {
      String param =
        parent.getClass().getPackage().getName() + ".audio.buffersize";
      int bufferSize = DefaultValueParser.parseInt(parent, param,
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
}
