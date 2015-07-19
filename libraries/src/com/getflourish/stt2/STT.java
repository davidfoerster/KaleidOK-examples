package com.getflourish.stt2;

import com.getflourish.stt2.mock.MockTranscriptionService;
import kaleidok.util.Timer;
import org.apache.http.concurrent.FutureCallback;

import java.net.URL;
import java.util.concurrent.TimeUnit;


public class STT
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

  private boolean isActive = false;

  private boolean shouldAutoRecord = false;
  private State status = State.IDLE, lastStatus = State.IDLE;

  protected final TranscriptionService service;
  private final AudioTranscriptionProcessor processor;

  private final Timer recordingTimer = new Timer();

  //private final VolumeThresholdTracker volumeThresholdTracker = new VolumeThresholdTracker(); // TODO: integration


  public static boolean debug;


  public STT( FutureCallback<SttResponse> resultHandler, String accessKey )
  {
    service = accessKey.startsWith("!MOCK") ?
      new MockTranscriptionService(accessKey, resultHandler) :
      new TranscriptionService(accessKey, resultHandler);
    processor = new AudioTranscriptionProcessor(this);

    //setAutoRecording(false);
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


  public void shutdown()
  {
    // TODO
    status = State.SHUTDOWN;
    service.shutdownNow();
  }


  public synchronized void begin( boolean doThrow )
  {
    if (isActive) {
      if (doThrow)
        throw new IllegalStateException("already running");
    } else {
      onBegin();
      isActive = true;
      shouldAutoRecord = false;
    }
  }


  /*
  public void setAutoRecording( boolean enable )
  {
    shouldAutoRecord = enable;
    setAutoThreshold(enable);
    statusText = enable ?
      "STT info: Automatic mode enabled. Anything louder than threshold will be recorded." :
      "STT info: Manual mode enabled. Use begin() / end() to manage recording.";
  }

  public void setAutoRecording( double threshold )
  {
    shouldAutoRecord = true;
    volumeThresholdTracker.setManualThreshold(threshold);
    statusText = "STT info: Automatic mode enabled. Anything louder than " + threshold + " will be recorded.";
  }

  public void setAutoThreshold( boolean enabled )
  {
    volumeThresholdTracker.setAutoThreshold(enabled);
  }

  private void draw()
  {
    if (shouldAutoRecord)
      handleAutoRecording();

    if (status != lastStatus)
    {
      dispatchTranscriptionEvent(thread.getUtterance(),
        thread.getConfidence(), thread.getLanguage(),
        status);
      lastStatus = status;
    }

    if (thread.isResultAvailable())
    {
      // Why dispatch the same event twice?
      dispatchTranscriptionEvent(thread.getUtterance(),
        thread.getConfidence(), thread.getLanguage(),
        thread.getStatus());

      statusText = "Listening";
      status = LISTENING;
      // Why dispatch the same event a third time?
      dispatchTranscriptionEvent(thread.getUtterance(),
        thread.getConfidence(), thread.getLanguage(), status);
      lastStatus = status;
      thread.interrupt();
    }

    if (debug && !statusText.equals(lastStatusText))
    {
      System.out.println(getTime() + ' ' + statusText);
      lastStatusText = statusText;
    }
  }
  */


  public synchronized void end( boolean doThrow )
  {
    if (!isActive) {
      if (doThrow)
        throw new IllegalStateException("not running");
    } else {
      onSpeechFinish();
      isActive = false;
      shouldAutoRecord = false;
    }
  }


  public State getStatus() {
    return status;
  }


  /*
  private final DateFormat timeFormat =  new SimpleDateFormat("HH:mm:ss");

  private String getTime()
  {
    return timeFormat.format(new Date());
  }
  */

  /*
  private void handleAutoRecording( AudioEvent audioEvent )
  {
    volumeThresholdTracker.process(audioEvent);

    if (audioEvent.getRMS() >= volumeThresholdTracker.getThreshold()) { // TODO: use common volume level calculator
      onSpeech();
    } else if (recordingTimer.isFinished()) {
      if (recorder.isRecording() && isRecording) {
        onSpeechFinish();
      } else {
        startListening();
      }
    }
  }
  */


  private synchronized void onBegin()
  {
    status = State.RECORDING;
    processor.shouldRecord = true;
    startListening();

    if (debug)
      System.out.println(status);
  }


  /*
  private void onSpeech()
  {
    statusText = "Recording";
    status = RECORDING;
    recordingTimer.start();
  }
  */


  public synchronized void onSpeechFinish()
  {
    status = State.IDLE;
    processor.shouldRecord = false;

    if (debug) {
      System.out.format("%s roughly %.3f seconds of audio data...%n",
        status, recordingTimer.getRuntime() * 1e-9);
    }

    recordingTimer.reset();
    //dispatchTranscriptionEvent("", 0, null, TRANSCRIBING);
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
}
