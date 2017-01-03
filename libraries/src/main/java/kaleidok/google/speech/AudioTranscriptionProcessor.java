package kaleidok.google.speech;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import javaFlacEncoder.FLACEncoder;
import javaFlacEncoder.FLACOutputStream;
import javaFlacEncoder.FLACStreamOutputStream;
import javaFlacEncoder.StreamConfiguration;
import kaleidok.util.Arrays;

import java.io.IOException;
import java.util.logging.Level;

import static kaleidok.google.speech.STT.logger;


public class AudioTranscriptionProcessor implements AudioProcessor
{
  private final STT stt;

  private StreamConfiguration streamConfiguration = null;

  private FLACEncoder encoder = new FLACEncoder();

  private FlacTranscription task = null;

  private int[] audioConversionBuffer = null;

  private double transcriptionEndTimestamp = Double.POSITIVE_INFINITY;

  private int intervalSequenceCount = 0;

  public volatile boolean shouldRecord = false;


  protected AudioTranscriptionProcessor( STT stt )
  {
    this.stt = stt;
  }


  @Override
  public boolean process( AudioEvent audioEvent )
  {
    if (shouldRecord)
    {
      int[] audioInt = convertTo16Bit(audioEvent, audioConversionBuffer);
      audioConversionBuffer = audioInt;
      try {
        ensureEncoderReady(audioEvent);
        encoder.addSamples(audioInt, audioInt.length);
        encoder.t_encodeSamples(
          encoder.fullBlockSamplesAvailableToEncode(),
          false, Integer.MAX_VALUE);

        if (audioEvent.getEndTimeStamp() < transcriptionEndTimestamp)
          return true;
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        if (task != null) {
          task.dispose();
          task = null;
        }
      }

      if (task != null) {
        logger.log(Level.FINER,
          "Speech recording interrupted at {0} of up to {1} intervals after " +
            "excess of the max. interval of {2,number,0.###} s",
          new Object[]{
            intervalSequenceCount, getIntervalSequenceCountMaxString(),
            stt.getMaxTranscriptionInterval()
          });
      }

      if (task != null && intervalSequenceCount < getIntervalSequenceCountMax()) {
        intervalSequenceCount++;
      } else {
        stt.end(false);
        intervalSequenceCount = 0;
      }
    }

    finishEncoding();
    return true;
  }


  private int getIntervalSequenceCountMax()
  {
    int n = stt.getIntervalSequenceCountMax();
    return (n > 0) ? n : Integer.MAX_VALUE;
  }


  private Object getIntervalSequenceCountMaxString()
  {
    int n = stt.getIntervalSequenceCountMax();
    // We need to perform the explicit cast inside the branches as to receive either an Integer or Character instance. Otherwise we'll always get Integer.
    //noinspection ConditionalExpressionWithIdenticalBranches
    return (n > 0 && n < Integer.MAX_VALUE) ? (Object) n : (Object) '∞';
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

    if (task == null) {
      task = new FlacTranscription(ev.getSampleRate());

      double maxTranscriptionInterval = stt.getMaxTranscriptionInterval();
      transcriptionEndTimestamp =
        (maxTranscriptionInterval > 0) ?
          ev.getTimeStamp() + maxTranscriptionInterval :
          Double.POSITIVE_INFINITY;
    }

    if (intervalSequenceCount == 0)
      intervalSequenceCount = 1;
  }


  private void finishEncoding()
  {
    transcriptionEndTimestamp = Double.POSITIVE_INFINITY;
    if (task != null) {
      try {
        task.finishEncoding();
      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        task = null;
      }
    }
  }


  @Override
  public void processingFinished()
  {
    //volumeThresholdTracker.processingFinished();
    finishEncoding();
  }


  private class FlacTranscription extends Transcription
  {
    private final FLACOutputStream outputStream;

    public FlacTranscription( float sampleRate ) throws IOException
    {
      super(stt.service.getServiceUri().toURL(), "audio/x-flac", sampleRate);
      callback = stt.service.resultHandler;
      logfilePathFormat = stt.getLogfilePathFormat();
      outputStream = new FLACStreamOutputStream(getOutputStream());
      encoder.setOutputStream(outputStream);
      encoder.openFLACStream();
    }

    public void finishEncoding() throws IOException
    {
      assert !stt.service.isInQueue(this) :
        this + " is not in the queue of " + stt.service;

      int availableSamples = encoder.samplesAvailableToEncode();
      logger.log(Level.FINEST,
        "{0,number} seconds left to encode after recording finished",
        (double) availableSamples / streamConfiguration.getSampleRate());

      long encodingStartTime = System.nanoTime();
      encoder.t_encodeSamples(availableSamples, true, availableSamples);
      logExcessDuration(encodingStartTime, stt.getMaxTranscriptionInterval(),
        "Encoding the remaining samples took too long");

      stt.service.execute(this);
    }


    @Override
    public void dispose()
    {
      try {
        outputStream.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        super.dispose();
      }
    }
  }


  private static void logExcessDuration( long startTimeNanos, double maxDuration,
    String message )
  {
    double duration = (System.nanoTime() - startTimeNanos) * 1e-9;
    if (duration >= maxDuration)
    {
     logger.log(Level.FINER, "{0}: {1,number,0.0000E0} seconds",
       new Object[]{ message, duration });
    }
  }


  private static final float maxSample = 2;

  private static int[] convertTo16Bit( AudioEvent ev, int[] conversionBuffer )
  {
    final float[] audioFloat = ev.getFloatBuffer();
    final int offset = ev.getOverlap();
    final int len = audioFloat.length - offset;
    final int[] audioInt =
      (conversionBuffer != null && len <= conversionBuffer.length) ?
        conversionBuffer :
        new int[len];
    for (int i = 0; i < len; i++) {
      final float sample = audioFloat[i + offset];
      assert Math.abs(sample) <= maxSample :
        getSampleValueErrorMessage(audioFloat, i + offset);
      audioInt[i] = (int) (sample * (Short.MAX_VALUE / maxSample));
    }
    return audioInt;
  }


  private static String getSampleValueErrorMessage( final float[] aSamples,
    int idx )
  {
    return String.format(
      "Encountered sample value %g at index %d; min=%g, max=%g",
      aSamples[idx], idx,
      Arrays.stream(aSamples).min().orElse(Double.NaN),
      Arrays.stream(aSamples).max().orElse(Double.NaN));
  }
}
