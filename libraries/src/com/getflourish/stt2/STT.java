package com.getflourish.stt2;

import com.getflourish.stt2.mock.MockTranscriptionService;
import com.getflourish.stt2.util.Timer;
import org.apache.http.concurrent.FutureCallback;

import java.net.URL;


public class STT
{
  public static final int LISTENING = 1;
  public static final int RECORDING = 2;
  public static final int TRANSCRIBING = 3;
  public static final int SUCCESS = 4;
  public static final int ERROR = 5;

  private boolean isActive = false, isRecording = false;

  private boolean shouldAutoRecord = false;
  private int  status = -1, lastStatus = -1;
  private String statusText = "", lastStatusText = "";

  protected final TranscriptionService service;
  private final AudioTranscriptionProcessor processor;

  private int interval = 500;
  private final Timer recordingTimer;

  //private final VolumeThresholdTracker volumeThresholdTracker = new VolumeThresholdTracker(); // TODO: integration


  public static boolean debug;


  public STT( FutureCallback<SttResponse> resultHandler, String accessKey )
  {
    service = accessKey.startsWith("!MOCK") ?
      new MockTranscriptionService(accessKey, resultHandler) :
      new TranscriptionService(accessKey, resultHandler);
    processor = new AudioTranscriptionProcessor(this);

    //setAutoRecording(false);

    recordingTimer = new Timer(interval);
    recordingTimer.start();
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

  public String getStatusText() {
    return statusText;
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
    statusText = "Recording";
    status = RECORDING;
    processor.shouldRecord = true;
    startListening();

    if (debug)
      System.out.println(statusText);
  }

  /*
  private void onSpeech()
  {
    statusText = "Recording";
    status = RECORDING;
    recordingTimer.start();
    isRecording = true;
  }
  */

  public synchronized void onSpeechFinish()
  {
    statusText = "Transcribing";
    status = TRANSCRIBING;
    processor.shouldRecord = false;
    isRecording = false;

    if (debug) {
      System.out.format("%s %.3f seconds of audio data...\n",
        statusText, recordingTimer.getRuntime() * 1e-9);
    }

    //dispatchTranscriptionEvent("", 0, null, TRANSCRIBING);
  }

  private void startListening()
  {
    // TODO: stop and then restart "recorder"
    recordingTimer.start();
  }
}
