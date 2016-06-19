package kaleidok.examples.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
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
import kaleidok.processing.Plugin;
import kaleidok.util.DefaultValueParser;
import processing.event.KeyEvent;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.Math.isPowerOfTwo;


public class AudioProcessingManager extends Plugin<Kaleidoscope>
{
  public static int DEFAULT_AUDIO_SAMPLERATE = 32000;
  public static int DEFAULT_AUDIO_BUFFERSIZE = 1 << 12;

  private int audioBufferSize = 0, audioBufferOverlap = 0;
  private AudioDispatcher audioDispatcher;
  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;
  private MinimFFTProcessor fftProcessor;


  AudioProcessingManager( Kaleidoscope sketch )
  {
    super(sketch);
  }


  @Override
  protected void onDispose()
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
      String paramBase = p.getClass().getPackage().getName() + ".audio.";
      String audioSource = p.getParameter(paramBase + "input");
      int bufferSize = getAudioBufferSize(),
        bufferOverlap = getAudioBufferOverlap();

      Runnable dispatcherRunnable = null;
      try {
        if (audioSource == null)
        {
          String param = paramBase + "samplerate";
          int sampleRate = DefaultValueParser.parseInt(p,
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
            replayAction = new ReplayAction(p, audioSource);
            ais = replayAction.audioInputStream;
            dispatcherRunnable = () ->
            {
              replayAction.doReplayItem(0);
              audioDispatcher.run();
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
    if (DefaultValueParser.parseBoolean(p,
      p.getClass().getPackage().getName() + ".audio.input.play", false))
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
      audioDispatcherThread = new Thread(() ->
      {
        p.getLayers().waitForImages();
        dispatcher.run();
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
        p.getClass().getPackage().getName() + ".audio.buffersize";
      int bufferSize = DefaultValueParser.parseInt(p, param,
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
        p.getClass().getPackage().getName() + ".audio.overlap";
      int bufferOverlap = DefaultValueParser.parseInt(p,
        param, bufferSize / 2);
      if (bufferOverlap < 0 || bufferOverlap >= bufferSize)
        throw new AssertionError(param + " must be positive and less than buffersize");
      if (!isPowerOfTwo(bufferOverlap))
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


  public static final class ReplayAction extends Plugin<Kaleidoscope>
    implements ActionListener
  {
    private final ReplayList replayList;

    public final MultiAudioInputStream audioInputStream;

    public final Timer timer;

    private volatile int idx = -1;

    private Boolean ignoreTranscriptionResult = null;


    private ReplayAction( Kaleidoscope sketch, String filename )
      throws IOException, JsonParseException, UnsupportedAudioFileException
    {
      this(sketch, TypeAdapterManager.getGson().fromJson(
        new FileReader(filename), ReplayList.class));
    }


    private ReplayAction( Kaleidoscope sketch, ReplayList replayList )
      throws IOException, JsonParseException, UnsupportedAudioFileException
    {
      super(sketch);

      this.replayList = replayList;
      audioInputStream = makeMultiAudioStream(replayList.items);
      timer = new Timer(0, this);
      timer.setRepeats(false);

      logger.log(Level.CONFIG,
        "Replaying recorded interaction \"{0}\"", replayList.name);
    }


    private static MultiAudioInputStream makeMultiAudioStream(
      ReplayList.Item[] items )
      throws IOException, UnsupportedAudioFileException
    {
      if (items.length == 0)
        throw new NoSuchElementException("Empty replay list");

      MultiAudioInputStream audioInputStream = new MultiAudioInputStream(
        new ArrayList<>(items.length));

      try
      {
        TarsosDSPAudioFormat format = null;
        for (ReplayList.Item item : items)
        {
          TarsosDSPAudioInputStream ais;
          {
            InputStream is = item.url.openStream();
            try
            {
              ais = new ContinuousAudioInputStream(is);
            }
            catch (IOException | UnsupportedAudioFileException ex)
            {
              is.close();
              throw ex;
            }
          }
          if (format == null)
          {
            format = ais.getFormat();
          }
          else if (!ais.getFormat().matches(format))
          {
            ais.close();
            throw new UnsupportedAudioFileException(String.format(
              "Mismatching audio formats: \"%s\" vs. \"%s\"",
              ais.getFormat(), format));
          }
          audioInputStream.streams.add(ais);
        }
      }
      catch (IOException | UnsupportedAudioFileException ex)
      {
        audioInputStream.clear();
        throw ex;
      }

      return audioInputStream;
    }


    @Override
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

      MultiAudioInputStream ais = audioInputStream;
      double streamLength =
        ais.getCurrentStream().getFrameLength() /
          (double) ais.getFormat().getSampleRate();
      timer.setInitialDelay((int)(streamLength * 1e3));
      timer.start();

      logger.log(Level.FINE,
        "Playing back audio file {0} of {1}",
        new Object[]{ais.getCurrentIdx() + 1, ais.streams.size()});
    }


    @Override
    public void actionPerformed( ActionEvent ev )
    {
      if (!isIgnoreTranscriptionResult()) {
        p.getChromasthetiationService()
          .submit(replayList.items[idx].transcription);
      }

      doNextReplayItem();
    }


    private boolean isIgnoreTranscriptionResult()
    {
      if (ignoreTranscriptionResult == null) {
        ignoreTranscriptionResult =
          SttManager.getParamIgnoreTranscriptionResult(p);
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
      public URL url;

      public String transcription;
    }
  }
}
