package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import com.google.gson.JsonParseException;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.MultiAudioInputStream;
import kaleidok.audio.OffThreadAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.util.DefaultValueParser;
import processing.event.KeyEvent;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.Math.isPowerOfTwo;


public class AudioProcessingManager
{
  public static int DEFAULT_AUDIO_SAMPLERATE = 32000;
  public static int DEFAULT_AUDIO_BUFFERSIZE = 1 << 12;

  private final Kaleidoscope parent;

  private int audioBufferSize = 0, audioBufferOverlap = 0;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;


  AudioProcessingManager( Kaleidoscope parent )
  {
    this.parent = parent;
    parent.registerMethod("dispose", this);
  }


  public void dispose()
  {
    if (audioDispatcher != null)
      audioDispatcher.stop();
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
      int bufferSize = getAudioBufferSize(),
        bufferOverlap = getAudioBufferOverlap();

      Runnable dispatcherRunnable = null;
      try {
        if (audioSource == null)
        {
          String param =  paramBase + "samplerate";
          int sampleRate = DefaultValueParser.parseInt(parent,
            param, DEFAULT_AUDIO_SAMPLERATE);
          if (sampleRate <= 0)
            throw new AssertionError(param + " must be positive");
          audioDispatcher =
            fromDefaultMicrophone(sampleRate, bufferSize, bufferOverlap);
          dispatcherRunnable = audioDispatcher;
        }
        else
        {
          TarsosDSPAudioInputStream ais;
          if (audioSource.endsWith(".json")) {
            replayAction = new ReplayAction(audioSource);
            ais = replayAction.audioInputStream;
            dispatcherRunnable = new Runnable()
              {
                @Override
                public void run()
                {
                  replayAction.doReplayItem(0);
                  audioDispatcher.run();
                }
              };
          } else {
            ais = new ContinuousAudioInputStream(audioSource);
          }
          audioDispatcher = new AudioDispatcher(ais, bufferSize, bufferOverlap);
          dispatcherRunnable = initAudioPlayer(audioDispatcher, dispatcherRunnable);
        }
      } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | JsonParseException ex) {
        throw new Error(ex);
      }

      audioDispatcher.addAudioProcessor(getVolumeLevelProcessor());
      audioDispatcher.addAudioProcessor(getFftProcessor());

      logger.config(audioDispatcher.getFormat().toString());
      initAudioDispatcherThread(dispatcherRunnable);
    }
  }


  private Runnable initAudioPlayer( AudioDispatcher audioDispatcher,
    Runnable chained )
    throws LineUnavailableException, IOException
  {
    if (DefaultValueParser.parseBoolean(parent,
      parent.getClass().getPackage().getName() + ".audio.input.play", false))
    {
      audioDispatcher.addAudioProcessor(new OffThreadAudioPlayer(
        JVMAudioInputStream.toAudioFormat(audioDispatcher.getFormat()),
        getAudioBufferSize() - getAudioBufferOverlap()));
    }
    return new DummyAudioPlayer().addToDispatcher(audioDispatcher, chained);
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


  public int getAudioBufferOverlap()
  {
    if (audioBufferOverlap <= 0)
    {
      int bufferSize = getAudioBufferSize();
      String param =
        parent.getClass().getPackage().getName() + ".audio.overlap";
      int bufferOverlap = DefaultValueParser.parseInt(parent,
        param, bufferSize / 2);
      if (bufferOverlap < 0 || bufferOverlap >= bufferSize)
        throw new AssertionError(param + " must be positive and less than buffersize");
      if (bufferOverlap > 0 && !isPowerOfTwo(bufferOverlap))
        throw new AssertionError(param + " must be a power of 2");
      audioBufferOverlap = bufferOverlap;
    }
    return audioBufferOverlap;
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


  private ReplayAction replayAction = null;


  public class ReplayAction implements ActionListener
  {
    public final ReplayList replayList;

    public final MultiAudioInputStream audioInputStream;

    public final Timer timer;

    private volatile int idx = -1;

    private Boolean ignoreTranscriptionResult = null;


    private ReplayAction( String filename )
      throws IOException, JsonParseException, UnsupportedAudioFileException
    {
      this(TypeAdapterManager.getGson().fromJson(
        new FileReader(filename), ReplayList.class));
    }


    private ReplayAction( ReplayList replayList )
      throws IOException, JsonParseException, UnsupportedAudioFileException
    {
      if (replayList.items.length == 0)
        throw new NoSuchElementException("Empty replay list");

      audioInputStream = new MultiAudioInputStream(
        new ArrayList<TarsosDSPAudioInputStream>(replayList.items.length));

      for (ReplayList.Item item: replayList.items) {
        audioInputStream.streams.add(
          new ContinuousAudioInputStream(new URL(item.url).openStream()));
      }

      if (!audioInputStream.isFormatCompatible(
        audioInputStream.streams.get(0).getFormat()))
      {
        throw new UnsupportedAudioFileException("Mismatching audio formats");
      }

      parent.registerMethod("keyEvent", this);
      timer = new Timer(0, this);
      timer.setRepeats(false);
      this.replayList = replayList;

      logger.log(Level.CONFIG,
        "Replaying recorded interaction \"{0}\"", replayList.name);
    }


    public void keyEvent( KeyEvent ev )
    {
      if (ev.getAction() == KeyEvent.TYPE) {
        switch (ev.getKey()) {
        case 'n':
          doNextReplayItem();
          break;
        }
      }
    }


    private boolean checkRunning()
    {
      if (timer.isRunning()) {
        System.err.println("Please wait for the current replay action to finish!");
        return true;
      }
      return false;
    }


    public void doNextReplayItem()
    {
      if (checkRunning())
        return;

      MultiAudioInputStream ais = audioInputStream;
      int idx = ais.getCurrentIdx() + 1;
      if (idx >= ais.streams.size()) {
        logger.info("Last replay item has been finished");
        return;
      }

      doReplayItem(idx);
    }


    public void doReplayItem( int idx )
    {
      if (checkRunning())
        return;

      try {
        audioInputStream.setCurrentIdx(idx);
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Couldn't skip to next replay item", ex);
        return;
      }
      this.idx = idx;
      doReplayItem();
    }


    public void doReplayItem()
    {
      if (checkRunning())
        return;

      double streamLength =
        audioInputStream.getCurrentStream().getFrameLength() /
          (double) audioInputStream.getFormat().getSampleRate();
      timer.setInitialDelay((int)(streamLength * 1e3));
      timer.start();
    }


    @Override
    public void actionPerformed( ActionEvent ev )
    {
      if (!isIgnoreTranscriptionResult()) {
        parent.getChromasthetiationService()
          .submit(replayList.items[idx].transcription);
      }

      doNextReplayItem();
    }


    private boolean isIgnoreTranscriptionResult()
    {
      if (ignoreTranscriptionResult == null) {
        ignoreTranscriptionResult =
          SttManager.getParamIgnoreTranscriptionResult(parent);
      }
      return ignoreTranscriptionResult;
    }
  }


  private static class ReplayList
  {
    public String name;

    public Item[] items;


    public static class Item
    {
      public String url, transcription;
    }
  }
}
