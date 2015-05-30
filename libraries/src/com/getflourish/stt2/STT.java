package com.getflourish.stt2;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import com.getflourish.stt2.mock.MockTranscriptionThread;
import com.getflourish.stt2.util.Timer;
import javaFlacEncoder.FLACEncoder;
import javaFlacEncoder.FLACOutputStream;
import javaFlacEncoder.FLACStreamOutputStream;
import javaFlacEncoder.StreamConfiguration;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class STT implements AudioProcessor
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

  private StreamConfiguration streamConfiguration = null;
  private FLACEncoder encoder = new FLACEncoder();
  private FLACOutputStream outputStream;
  private final TranscriptionThread transcriptionThread;

  private int interval = 500;
  private final Timer recordingTimer;

  //private final VolumeThresholdTracker volumeThresholdTracker = new VolumeThresholdTracker(); // TODO: integration

  private boolean debug;

  public STT( TranscriptionResultHandler resultHandler, String accessKey )
  {
    transcriptionThread =
      ("!MOCK".equals(accessKey)) ?
        new MockTranscriptionThread(accessKey, resultHandler) :
        new TranscriptionThread(accessKey, resultHandler);
    transcriptionThread.start();

    //setAutoRecording(false);

    recordingTimer = new Timer(interval);
    recordingTimer.start();
  }

  public void setLanguage( String language )
  {
    transcriptionThread.language = language;
  }

  public void begin( boolean doThrow )
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
  */

  public void setDebug( boolean b ) {
    transcriptionThread.debug = debug = b;
  }

  /*
  private void draw()
  {
    if (shouldAutoRecord)
      handleAutoRecording();

    if (status != lastStatus)
    {
      dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
        transcriptionThread.getConfidence(), transcriptionThread.getLanguage(),
        status);
      lastStatus = status;
    }

    if (transcriptionThread.isResultAvailable())
    {
      // Why dispatch the same event twice?
      dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
        transcriptionThread.getConfidence(), transcriptionThread.getLanguage(),
        transcriptionThread.getStatus());

      statusText = "Listening";
      status = LISTENING;
      // Why dispatch the same event a third time?
      dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
        transcriptionThread.getConfidence(), transcriptionThread.getLanguage(), status);
      lastStatus = status;
      transcriptionThread.interrupt();
    }

    if (debug && !statusText.equals(lastStatusText))
    {
      System.out.println(getTime() + ' ' + statusText);
      lastStatusText = statusText;
    }
  }
  */

  public void end( boolean doThrow )
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

  private void onBegin()
  {
    statusText = "Recording";
    status = RECORDING;
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

  public void onSpeechFinish()
  {
    statusText = "Transcribing";
    status = TRANSCRIBING;
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

  // TODO: Move to separate (inner) class
  @Override
  public boolean process( AudioEvent audioEvent )
  {
    if (status == RECORDING)
    {
      int[] audioInt = convertTo16Bit(audioEvent, audioConversionBuffer);
      audioConversionBuffer = audioInt;
      try {
        ensureEncoderReady(audioEvent);
        encoder.addSamples(audioInt, audioInt.length);
        encoder.t_encodeSamples(
          encoder.fullBlockSamplesAvailableToEncode(),
          false, Integer.MAX_VALUE);
      }
      catch (IOException ex1) {
        handleIoException(ex1);
      }
    }
    else
    {
      finishEncoding();
    }
    return true;
  }

  private void ensureEncoderConfigured( AudioEvent ev )
  {
    if (streamConfiguration == null) {
      int blockSize = ev.getBufferSize() - ev.getOverlap();
      streamConfiguration =
        new StreamConfiguration(1, blockSize,
          Math.max(blockSize, StreamConfiguration.DEFAULT_MAX_BLOCK_SIZE),
          (int) ev.getSampleRate(), Short.SIZE);
      encoder.setStreamConfiguration(streamConfiguration);
      encoder.setThreadCount(1);
    }
  }

  private void ensureEncoderReady( AudioEvent ev ) throws IOException
  {
    ensureEncoderConfigured(ev);

    if (outputStream == null) {
      PipedOutputStream outputStream = new PipedOutputStream();
      transcriptionThread.attachInput(
        new PipedInputStream(outputStream), "audio/x-flac",
        ev.getSampleRate());
      this.outputStream = new FLACStreamOutputStream(outputStream);
      encoder.setOutputStream(this.outputStream);
      encoder.openFLACStream();
    }
  }

  private int[] audioConversionBuffer = null;

  private static int[] convertTo16Bit( AudioEvent ev, int[] conversionBuffer )
  {
    final float[] audioFloat = ev.getFloatBuffer();
    final int len = audioFloat.length - ev.getOverlap();
    final int[] audioInt;
    if (conversionBuffer != null && len <= conversionBuffer.length) {
      audioInt = conversionBuffer;
    } else {
      audioInt = new int[len];
    }
    final int offset = ev.getOverlap() / 2;
    for (int i = 0; i < len; i++)
      audioInt[i] = (int) (audioFloat[i + offset] * Short.MAX_VALUE);
    return audioInt;
  }

  private void handleIoException( IOException ex1 )
  {
    ex1.printStackTrace();
    if (outputStream != null) {
      try {
        outputStream.close();
        outputStream = null;
      } catch (IOException ex2) {
        ex2.printStackTrace();
      }
    }
    end(false);
  }

  private void finishEncoding()
  {
    if (outputStream != null) {
      try {
        int availableSamples = encoder.samplesAvailableToEncode();
        if (debug) {
          double availableLength = (double) availableSamples / streamConfiguration.getSampleRate();
          System.out.format("%.4g seconds left to encode after recording finished\n", availableLength);
        }

        long now = System.nanoTime();
        encoder.t_encodeSamples(availableSamples, true, Integer.MAX_VALUE); // TODO: dispatch to other thread
        if (debug)
          assertShorter(now, 10 * 1000000L, "encoding remaining samples took too long");
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      outputStream = null;
    }
  }

  @Override
  public void processingFinished()
  {
    //volumeThresholdTracker.processingFinished();
    finishEncoding();
  }

  private static boolean assertShorter( long startTime, long maxDuration, String message )
  {
    if (System.nanoTime() - startTime < maxDuration)
      return true;
    //throw new AssertionError(message);
    System.err.println(message);
    return false;
  }
}
