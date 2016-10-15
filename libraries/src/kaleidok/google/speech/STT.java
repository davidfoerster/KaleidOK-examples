package kaleidok.google.speech;

import kaleidok.google.speech.mock.MockTranscriptionService;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.util.Timer;
import org.apache.http.concurrent.FutureCallback;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.net.URL;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
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

  public int intervalSequenceCountMax = 1;

  private boolean isActive = false;

  private State status = State.IDLE;

  protected final TranscriptionService service;
  private final AudioTranscriptionProcessor processor;

  private final Timer recordingTimer = new Timer();

  //private final VolumeThresholdTracker volumeThresholdTracker = new VolumeThresholdTracker(); // TODO: integration

  private final Collection<ChangeListener> changeListeners = new ArrayList<>();

  private final ChangeEvent changeEvent = new ChangeEvent(this);

  public Format logfilePattern = null;


  static final Logger logger = Logger.getLogger(STT.class.getPackage().getName());


  public STT( FutureCallback<SttResponse> resultHandler, String accessKey )
  {
    service = accessKey.startsWith("!MOCK") ?
      new MockTranscriptionService(accessKey, resultHandler, this) :
      new TranscriptionService(accessKey, resultHandler);
    processor = new AudioTranscriptionProcessor(this);
  }


  public AudioTranscriptionProcessor getAudioProcessor()
  {
    return processor;
  }


  public URL getApiBase()
  {
    return service.getApiBase();
  }

  public void setApiBase( URL apiBase )
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


  private void signalChange()
  {
    for (ChangeListener listener: changeListeners)
      listener.stateChanged(changeEvent);
  }


  public void shutdown()
  {
    // TODO
    status = State.SHUTDOWN;
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


  public State getStatus() {
    return status;
  }


  static final Level statusLoggingLevel = Level.INFO;

  private synchronized void onBegin()
  {
    status = State.RECORDING;
    processor.shouldRecord = true;
    startListening();

    logger.log(statusLoggingLevel, status.name());

    signalChange();
  }


  public synchronized void onSpeechFinish()
  {
    status = State.IDLE;
    processor.shouldRecord = false;

    logger.log(Level.FINER,
      "{0} roughly {1,number,0.000} seconds of audio data",
      new Object[]{status, recordingTimer.getRuntime() * 1e-9});

    recordingTimer.reset();
    signalChange();
  }


  private void startListening()
  {
    // TODO: stop and then restart "recorder"
    recordingTimer.start();
  }


  public long getMaxTranscriptionInterval()
  {
    return recordingTimer.getTotalTime();
  }

  public void setMaxTranscriptionInterval( long interval, TimeUnit unit )
  {
    if (isRecording()) {
      throw new IllegalStateException(
        "Cannot change the transcription interval while a transcription is running");
    }
    recordingTimer.reset(interval, unit);
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
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return service.getPreferenceAdapters();
  }
}
