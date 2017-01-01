package kaleidok.google.speech;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.google.speech.mock.MockTranscriptionService;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.javafx.beans.property.binding.MessageFormatBinding;
import kaleidok.text.IMessageFormat;
import kaleidok.util.Timer;
import org.apache.http.concurrent.FutureCallback;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class STT implements PreferenceBean
{
  public enum State
  {
    IDLE,
    LISTENING,
    RECORDING,
    TRANSCRIBING,
    SUCCESS,
    ERROR,
    SHUTDOWN
  }

  private final AspectedIntegerProperty intervalSequenceCountMax;

  private boolean isActive = false;

  private final ReadOnlyObjectWrapper<State> status =
    new ReadOnlyObjectWrapper<>(this, "status", State.IDLE);

  protected final TranscriptionServiceBase service;

  private final AudioTranscriptionProcessor processor;

  private final Timer recordingTimer = new Timer();

  //private final VolumeThresholdTracker volumeThresholdTracker = new VolumeThresholdTracker(); // TODO: integration

  private final Collection<ChangeListener> changeListeners = new ArrayList<>();

  private final ChangeEvent changeEvent = new ChangeEvent(this);

  private final AspectedStringProperty logfilePathFormatString;

  private final MessageFormatBinding logfilePathFormat;


  static final Logger logger = Logger.getLogger(STT.class.getPackage().getName());


  public STT( FutureCallback<SttResponse> resultHandler, String accessKey )
  {
    service = accessKey.startsWith("!MOCK") ?
      MockTranscriptionService.newInstance(accessKey, resultHandler, this) :
      new TranscriptionService(accessKey, resultHandler);
    processor = new AudioTranscriptionProcessor(this);

    intervalSequenceCountMax = new AspectedIntegerProperty(
      this, "max. automatic sequential recordings", 1);
    intervalSequenceCountMax.addAspect(BoundedIntegerTag.getIntegerInstance(),
      new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
    intervalSequenceCountMax.addAspect(
      PropertyPreferencesAdapterTag.getInstance());

    logfilePathFormatString =
      new AspectedStringProperty(this, "log file path format");
    logfilePathFormatString.addAspect(
      PropertyPreferencesAdapterTag.getInstance());
    logfilePathFormat = new MessageFormatBinding(logfilePathFormatString);
    logfilePathFormat.testArgs = new Object[]{ new Date(0) };
  }


  public AudioTranscriptionProcessor getAudioProcessor()
  {
    return processor;
  }


  public URI getApiBase()
  {
    return service.getApiBase();
  }

  public void setApiBase( URI apiBase )
  {
    service.setApiBase(apiBase);
  }


  public String getAccessKey()
  {
    return service.getAccessKey();
  }

  public void setAccessKey( String accessKey )
  {
    service.setAccessKey(accessKey);
  }


  public String getLanguage()
  {
    return service.getLanguage();
  }

  public void setLanguage( String language )
  {
    service.setLanguage(language);
  }


  public void addChangeListener( ChangeListener listener )
  {
    changeListeners.add(listener);
  }

  public boolean removeChangeListener( ChangeListener listener )
  {
    return changeListeners.remove(listener);
  }


  public IntegerProperty intervalSequenceCountMaxProperty()
  {
    return intervalSequenceCountMax;
  }

  public int getIntervalSequenceCountMax()
  {
    return intervalSequenceCountMax.get();
  }

  public void setIntervalSequenceCountMax( int n )
  {
    intervalSequenceCountMaxProperty().set(n);
  }


  public StringProperty logfilePathFormatStringProperty()
  {
    return logfilePathFormatString;
  }

  public String getLogfilePathFormatString()
  {
    return logfilePathFormatString.get();
  }

  public void setLogfilePathFormatString( String format )
  {
    logfilePathFormatString.set(format);
  }


  public ObservableObjectValue<MessageFormat> logfilePathFormatProperty()
  {
    return logfilePathFormat;
  }

  public IMessageFormat getLogfilePathFormat()
  {
    return logfilePathFormat.asReadOnlyFormat();
  }


  private void signalChange()
  {
    for (ChangeListener listener: changeListeners)
      listener.stateChanged(changeEvent);
  }


  public void shutdown()
  {
    status.set(State.SHUTDOWN);
    service.shutdownNow();
    signalChange();
  }


  public synchronized void begin( boolean doThrow )
  {
    if (isActive) {
      if (doThrow)
        throw new IllegalStateException("already running");
    } else {
      onBegin();
      isActive = true;
    }
  }


  public synchronized void end( boolean doThrow )
  {
    if (!isActive) {
      if (doThrow)
        throw new IllegalStateException("not running");
    } else {
      onSpeechFinish();
      isActive = false;
    }
  }


  public ReadOnlyObjectProperty<State> statusProperty()
  {
    return status.getReadOnlyProperty();
  }

  public State getStatus()
  {
    return status.get();
  }


  static final Level statusLoggingLevel = Level.INFO;

  private synchronized void onBegin()
  {
    status.set(State.RECORDING);
    processor.shouldRecord = true;
    startListening();

    logger.log(statusLoggingLevel, status.get().name());

    signalChange();
  }


  public synchronized void onSpeechFinish()
  {
    status.set(State.IDLE);
    processor.shouldRecord = false;

    logger.log(Level.FINER,
      "{0} roughly {1,number,0.000} seconds of audio data",
      new Object[]{status, recordingTimer.getRuntime() * 1e-9});

    recordingTimer.reset();
    signalChange();
  }


  private void startListening()
  {
    recordingTimer.start();
  }


  private final AspectedDoubleProperty maxTranscriptionInterval =
    new AspectedDoubleProperty(this, "max. transcription interval", 0)
    {
      {
        DoubleSpinnerValueFactory bounds =
          new DoubleSpinnerValueFactory(0, Double.MAX_VALUE);
        bounds.setAmountToStepBy(0.5);
        addAspect(BoundedDoubleTag.getDoubleInstance(), bounds);
        addAspect(PropertyPreferencesAdapterTag.getInstance());

        invalidated();
      }


      public long getAsTotalRecorderTime()
      {
        double seconds = get();
        return
          (seconds > 0 && Double.isFinite(seconds)) ?
            (long) (seconds * 1e9) :
            -1;
      }


      @Override
      protected void invalidated()
      {
        Timer recordingTimer = STT.this.recordingTimer;
        long newValueNanos = getAsTotalRecorderTime();
        if (newValueNanos != recordingTimer.getTotalTime())
        {
          if (recordingTimer.isStarted())
          {
            throw new IllegalStateException(
              "Cannot change the max. transcription interval while a " +
                "transcription is running");
          }

          recordingTimer.reset(newValueNanos, TimeUnit.NANOSECONDS);
        }
      }
    };


  public DoubleProperty maxTranscriptionIntervalProperty()
  {
    return maxTranscriptionInterval;
  }

  public double getMaxTranscriptionInterval()
  {
    return maxTranscriptionInterval.get();
  }

  public void setMaxTranscriptionInterval( double interval )
  {
    maxTranscriptionInterval.set(interval);
  }

  public void setMaxTranscriptionInterval( long interval, TimeUnit unit )
  {
    setMaxTranscriptionInterval(unit.toNanos(interval) * 1e-9);
  }


  public boolean isRecording()
  {
    return recordingTimer.isStarted();
  }


  public static boolean isLoggingStatus()
  {
    return logger.isLoggable(statusLoggingLevel);
  }


  @Override
  public String getName()
  {
    return "Speech-to-Text";
  }


  @Override
  public Object getParent()
  {
    return null;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.concat(
      service.getPreferenceAdapters(),
      Stream.of(
        maxTranscriptionInterval.getAspect(
          PropertyPreferencesAdapterTag.getWritableInstance()),
        intervalSequenceCountMax.getAspect(
          PropertyPreferencesAdapterTag.getWritableInstance()),
        logfilePathFormatString.getAspect(
          PropertyPreferencesAdapterTag.getWritableInstance())));
  }
}
