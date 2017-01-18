package kaleidok.kaleidoscope;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import com.google.gson.JsonParseException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.audio.ContinuousAudioInputStream;
import kaleidok.audio.DummyAudioPlayer;
import kaleidok.audio.MultiAudioInputStream;
import kaleidok.audio.OffThreadAudioPlayer;
import kaleidok.audio.processor.MinimFFTProcessor;
import kaleidok.audio.processor.VolumeLevelProcessor;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.javafx.beans.property.AspectedBooleanProperty;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.RestartRequiredTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.javafx.scene.control.cell.SteppingIntegerSpinnerValueFactory;
import kaleidok.javafx.scene.control.cell.SteppingIntegerSpinnerValueFactory.BinaryLogarithmStepFunction;
import kaleidok.javafx.util.converter.IntegerNumberStringConverter;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.Plugin;
import kaleidok.text.InternationalSystemOfUnitsFormat;
import kaleidok.util.Strings;
import processing.event.KeyEvent;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.stream.Stream;

import static be.tarsos.dsp.io.jvm.AudioDispatcherFactory.fromDefaultMicrophone;
import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static kaleidok.util.AbstractResourceWrapper.closeExceptional;
import static kaleidok.util.Math.isPowerOfTwo;


public class AudioProcessingManager extends Plugin<Kaleidoscope>
  implements PreferenceBean
{
  private static int DEFAULT_AUDIO_SAMPLERATE = 32000;

  private static int DEFAULT_AUDIO_BUFFERSIZE = 1 << 12;


  private AudioDispatcher audioDispatcher;

  private Thread audioDispatcherThread;

  private VolumeLevelProcessor volumeLevelProcessor;

  private MinimFFTProcessor fftProcessor;

  private final AspectedIntegerProperty audioSampleRate;

  private final AspectedIntegerProperty audioBufferSize;

  private final AspectedIntegerProperty audioBufferOverlap;

  private final AspectedBooleanProperty audioPlaybackEnabled;


  AudioProcessingManager( Kaleidoscope sketch )
  {
    super(sketch);

    audioSampleRate =
      new AspectedIntegerProperty(this, "sample rate",
        loadAudioSampleRate(sketch));
    IntegerSpinnerValueFactory bounds =
      new IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
    bounds.setAmountToStepBy(1000);
    bounds.setConverter(new IntegerNumberStringConverter(
      InternationalSystemOfUnitsFormat.getNumberInstance(" Hz")));
    audioSampleRate.addAspect(BoundedIntegerTag.getIntegerInstance(), bounds);
    audioSampleRate.addAspect(LevelOfDetailTag.getInstance()).set(200);
    audioSampleRate
      .addAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
    audioSampleRate
      .addAspect(RestartRequiredTag.getInstance())
      .disarm();

    audioBufferSize =
      new AspectedIntegerProperty(this, "buffer size",
        loadAudioBufferSize(sketch));
    bounds =
      new SteppingIntegerSpinnerValueFactory(1,
        BinaryLogarithmStepFunction.INSTANCE.coerceToStep(Integer.MAX_VALUE),
        BinaryLogarithmStepFunction.INSTANCE);
    // TODO: use formatter with conversion to buffer period with current sampling rate
    audioBufferSize.addAspect(BoundedIntegerTag.getIntegerInstance(), bounds);
    audioBufferSize.addAspect(LevelOfDetailTag.getInstance()).set(1000);
    audioBufferSize
      .addAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
    audioBufferSize
      .addAspect(RestartRequiredTag.getInstance())
      .disarm();

    audioBufferOverlap =
      new AspectedIntegerProperty(this, "buffer overlap",
        loadAudioBufferOverlap(sketch, audioBufferSize.get()));
    bounds =
      new SteppingIntegerSpinnerValueFactory(0, Integer.MAX_VALUE,
        BinaryLogarithmStepFunction.INSTANCE);
    bounds.maxProperty().bind(audioBufferSize.divide(2));
    // TODO: use formatter with conversion to buffer period with current sampling rate
    audioBufferOverlap.addAspect(
      BoundedIntegerTag.getIntegerInstance(), bounds);
    audioBufferOverlap.addAspect(LevelOfDetailTag.getInstance()).set(1001);
    audioBufferOverlap
      .addAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
    audioBufferOverlap
      .addAspect(RestartRequiredTag.getInstance())
      .disarm();

    audioPlaybackEnabled =
      new AspectedBooleanProperty(this, "enable playback", false);
    audioPlaybackEnabled.addAspect(LevelOfDetailTag.getInstance()).set(100);
    audioPlaybackEnabled
      .addAspect(PropertyPreferencesAdapterTag.getWritableInstance())
      .load();
    audioPlaybackEnabled
      .addAspect(RestartRequiredTag.getInstance())
      .disarm();
  }


  @Override
  public synchronized void dispose()
  {
    if (audioDispatcher != null)
      audioDispatcher.stop();

    getPreferenceAdapters().forEach(PropertyPreferencesAdapter::save);

    super.dispose();
  }


  synchronized AudioDispatcher getAudioDispatcher()
  {
    initAudioDispatcher();
    return audioDispatcher;
  }


  synchronized Thread getAudioDispatcherThread()
  {
    initAudioDispatcher();
    return audioDispatcherThread;
  }


  private synchronized boolean isAudioDispatcherInitialized()
  {
    return audioDispatcher != null;
  }


  private synchronized void initAudioDispatcher()
  {
    if (audioDispatcher == null)
    {
      String audioSource =
        p.getParameterMap().get(
          p.getClass().getPackage().getName() + ".audio.input");
      int
        sampleRate =
          audioSampleRate.getAspect(RestartRequiredTag.getInstance())
            .updateReferenceValue().intValue(),
        bufferSize =
          audioBufferSize.getAspect(RestartRequiredTag.getInstance())
            .updateReferenceValue().intValue(),
        bufferOverlap =
          audioBufferOverlap.getAspect(RestartRequiredTag.getInstance())
            .updateReferenceValue().intValue();

      Runnable dispatcherRunnable = null;
      try
      {
        if (audioSource == null)
        {
          audioDispatcher =
            fromDefaultMicrophone(sampleRate, bufferSize, bufferOverlap);
          dispatcherRunnable = audioDispatcher;
        }
        else
        {
          TarsosDSPAudioInputStream ais;
          if (Strings.endsWith(audioSource, ".json", true))
          {
            replayAction = new ReplayAction(p, audioSource);
            ais = replayAction.audioInputStream;
            dispatcherRunnable = () -> {
                getReplayAction().doReplayItem(0);
                getAudioDispatcher().run();
              };
          }
          else
          {
            //noinspection resource,IOResourceOpenedButNotSafelyClosed
            ais = new ContinuousAudioInputStream(audioSource);
          }
          audioDispatcher =
            new AudioDispatcher(ais, bufferSize, bufferOverlap);
          dispatcherRunnable =
            initAudioPlayer(audioDispatcher, dispatcherRunnable);
        }
      }
      catch (JsonParseException ex)
      {
        throw new Error(ex);
      }
      catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex)
      {
        throw new IOError(ex);
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
    if (audioPlaybackEnabled
      .getAspect(RestartRequiredTag.getInstance()).updateReferenceValue())
    {
      OffThreadAudioPlayer player = new OffThreadAudioPlayer(
        JVMAudioInputStream.toAudioFormat(audioDispatcher.getFormat()),
        getDispatcherBufferSize() - getDispatcherBufferOverlap());
      player.offThread.setDaemon(true);
      player.offThread.start();
      audioDispatcher.addAudioProcessor(player);
    }
    return new DummyAudioPlayer().addToDispatcher(audioDispatcher, chained);
  }


  private void initAudioDispatcherThread( final Runnable dispatcher )
  {
    if (audioDispatcherThread == null)
    {
      audioDispatcherThread = new Thread(
        () -> {
          try
          {
            p.getLayers().waitForImages();
          }
          catch (RuntimeException ex)
          {
            logger.log(Level.WARNING,
              "Something unexpected happened while the audio dispatcher " +
                "waited for the sketch layers to become ready.",
              ex);
          }

          dispatcher.run();
        },
        "Audio dispatching");

      audioDispatcherThread.setDaemon(true);
      audioDispatcherThread.setPriority(Thread.NORM_PRIORITY + 1);
    }
  }


  public BooleanProperty audioPlaybackEnabledProperty()
  {
    return audioPlaybackEnabled;
  }

  public boolean isAudioPlaybackEnabled()
  {
    return
      audioPlaybackEnabled.getAspect(RestartRequiredTag.getInstance())
        .getReferenceValue();
  }

  public synchronized void setAudioPlaybackEnabled( boolean enabled )
  {
    checkCanChangeSamplingParameters();
    audioPlaybackEnabled.set(enabled);
  }


  public IntegerProperty audioBufferSizeProperty()
  {
    return audioBufferSize;
  }

  public int getDispatcherBufferSize()
  {
    return
      audioBufferSize.getAspect(RestartRequiredTag.getInstance())
        .getReferenceValue().intValue();
  }

  public synchronized void setDispatcherBufferSize( int bufferSize )
  {
    checkCanChangeSamplingParameters();
    audioBufferSize.set(verifyAudioBufferSize(bufferSize));
  }


  private static int loadAudioBufferSize( ExtPApplet p )
  {
    @SuppressWarnings("SpellCheckingInspection")
    String param =
      p.getClass().getPackage().getName() + ".audio.buffersize";
    String strBufferSize = p.getParameterMap().get(param);
    if (strBufferSize != null && !strBufferSize.isEmpty())
    {
      int bufferSize = Integer.parseInt(strBufferSize);
      if (bufferSize > 0 && isPowerOfTwo(bufferSize))
        return bufferSize;

      logger.log(Level.WARNING,
        "Ignoring property entry {0}: Buffer size {1} is no positive " +
          "power of 2",
        new Object[]{ param, bufferSize });
    }
    return getDefaultAudioBufferSize();
  }


  public IntegerProperty bufferBufferOverlapProperty()
  {
    return audioBufferOverlap;
  }

  public int getDispatcherBufferOverlap()
  {
    return
      audioBufferOverlap.getAspect(RestartRequiredTag.getInstance())
        .getReferenceValue().intValue();
  }

  public synchronized void setDispatcherBufferOverlap( int bufferOverlap )
  {
    checkCanChangeSamplingParameters();
    if (!verifyAudioBufferOverlap(bufferOverlap, audioBufferSize.get()))
    {
      throw new IllegalArgumentException(
        "Buffer overlap is no positive power of 2 less than the buffer size");
    }

    audioBufferOverlap.set(bufferOverlap);
  }


  private static int loadAudioBufferOverlap( ExtPApplet p, int bufferSize )
  {
    String param =
      p.getClass().getPackage().getName() + ".audio.overlap";
    String strBufferOverlap = p.getParameterMap().get(param);
    if (strBufferOverlap != null && !strBufferOverlap.isEmpty())
    {
      int bufferOverlap = Integer.parseInt(strBufferOverlap);
      if (verifyAudioBufferOverlap(bufferOverlap, bufferSize))
        return bufferOverlap;

      logger.log(Level.WARNING,
        "Ignoring property entry {0}: Buffer overlap {1} is no positive " +
          "power of 2 less than the buffer size ({2})",
        new Object[]{ param, bufferOverlap, bufferSize });
    }
    return bufferSize / 2;
  }


  private static boolean verifyAudioBufferOverlap( int bufferOverlap,
    int bufferSize )
  {
    return bufferOverlap >= 0 && bufferOverlap < bufferSize && isPowerOfTwo(bufferOverlap);
  }


  public IntegerProperty sampleRateProperty()
  {
    return audioSampleRate;
  }

  public int getDispatcherSampleRate()
  {
    return
      audioSampleRate.getAspect(RestartRequiredTag.getInstance())
        .getReferenceValue().intValue();
  }

  public synchronized void setSampleRate( int sampleRate )
  {
    checkCanChangeSamplingParameters();
    audioSampleRate.set(sampleRate);
  }


  private static int loadAudioSampleRate( ExtPApplet p )
  {
    @SuppressWarnings("SpellCheckingInspection")
    String param = p.getClass().getPackage().getName() + ".audio.samplerate";
    String strSampleRate = p.getParameterMap().get(param);
    if (strSampleRate != null && !strSampleRate.isEmpty())
    {
      int sampleRate = Integer.parseInt(strSampleRate);
      if (sampleRate > 0)
        return sampleRate;

      logger.log(Level.WARNING,
        "Ignoring property entry {0}: Sampling rate {1} isn''t positive",
        new Object[]{ param, sampleRate });
    }
    return getDefaultAudioSampleRate();
  }


  private void checkCanChangeSamplingParameters()
  {
    if (isAudioDispatcherInitialized())
    {
      throw new IllegalStateException(
        "Cannot change sampling parameters after audio dispatcher " +
          "initialization");
    }
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
      fftProcessor = new MinimFFTProcessor(getDispatcherBufferSize());
    return fftProcessor;
  }


  private ReplayAction replayAction = null;


  private synchronized ReplayAction getReplayAction()
  {
    initAudioDispatcher();
    return replayAction;
  }


  @Override
  public String getName()
  {
    return "Audio processing manager";
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return
      Stream.of(
        audioSampleRate, audioBufferSize, audioBufferOverlap,
        audioPlaybackEnabled)
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny);
  }


  public static int getDefaultAudioSampleRate()
  {
    return DEFAULT_AUDIO_SAMPLERATE;
  }

  public static void setDefaultAudioSampleRate( int defaultAudioSampleRate )
  {
    if (defaultAudioSampleRate <= 0)
      throw new IllegalArgumentException("Sampling rate must be positive");

    DEFAULT_AUDIO_SAMPLERATE = defaultAudioSampleRate;
  }


  public static int getDefaultAudioBufferSize()
  {
    return DEFAULT_AUDIO_BUFFERSIZE;
  }

  public static void setDefaultAudioBufferSize( int defaultAudioBufferSize )
  {
    DEFAULT_AUDIO_BUFFERSIZE = verifyAudioBufferSize(defaultAudioBufferSize);
  }

  private static int verifyAudioBufferSize( int bufferSize )
  {
    if (bufferSize > 0 && isPowerOfTwo(bufferSize))
      return bufferSize;

    throw new IllegalArgumentException(
      "Audio buffer size must be a positive power of 2");
  }


  public static final class ReplayAction extends Plugin<Kaleidoscope>
    implements ActionListener
  {
    private final ReplayList replayList;

    public final MultiAudioInputStream audioInputStream;

    public final Timer timer;

    private volatile int idx = -1;


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
              //noinspection resource,IOResourceOpenedButNotSafelyClosed
              ais = new ContinuousAudioInputStream(is);
            }
            catch (IOException | UnsupportedAudioFileException ex)
            {
              closeExceptional(is, ex);
              throw ex;
            }
          }
          if (format == null)
          {
            format = ais.getFormat();
          }
          else if (!ais.getFormat().matches(format))
          {
            throw closeExceptional(ais,
              new UnsupportedAudioFileException(String.format(
                "Mismatching audio formats: \"%s\" vs. \"%s\"",
                ais.getFormat(), format)));
          }
          audioInputStream.streams.add(ais);
        }
      }
      catch (IOException | UnsupportedAudioFileException ex)
      {
        closeExceptional(audioInputStream::clear, ex);
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
        logger.log(Level.SEVERE, "Couldn''t skip to next replay item", ex);
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
      //noinspection resource
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
      if (!p.getSTT().enableResponseHandlerProperty().get())
      {
        p.getChromasthetiationService()
          .submit(replayList.items[idx].transcription);
      }

      doNextReplayItem();
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
