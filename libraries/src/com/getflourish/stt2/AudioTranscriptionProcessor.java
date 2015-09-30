package com.getflourish.stt2;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import javaFlacEncoder.FLACEncoder;
import javaFlacEncoder.FLACOutputStream;
import javaFlacEncoder.FLACStreamOutputStream;
import javaFlacEncoder.StreamConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class AudioTranscriptionProcessor implements AudioProcessor
{
  private final STT stt;

  private StreamConfiguration streamConfiguration = null;

  private FLACEncoder encoder = new FLACEncoder();

  private FlacTranscription task = null;

  private int[] audioConversionBuffer = null;

  private double transcriptionEndTimestamp = Double.POSITIVE_INFINITY;

  public volatile boolean shouldRecord = false;


  protected AudioTranscriptionProcessor( STT stt )
  {
    this.stt = stt;
  }


  @Override
  public boolean process( AudioEvent audioEvent )
  {
    if (shouldRecord) {
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

        if (STT.debug) {
          System.out.format(
            "Speech recording interrupted after excess of the max. interval of %.3f s.%n",
            stt.getMaxTranscriptionInterval() * 1e-9);
        }
      } catch (IOException ex) {
        ex.printStackTrace();
        if (task != null) {
          task.dispose();
          task = null;
        }
      }
      stt.end(false);
    }

    finishEncoding();
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

    if (task == null) {
      task = new FlacTranscription(ev.getSampleRate());

      long maxTranscriptionInterval = stt.getMaxTranscriptionInterval();
      transcriptionEndTimestamp =
        (maxTranscriptionInterval > 0) ?
          ev.getTimeStamp() + maxTranscriptionInterval * 1e-9 :
          Double.POSITIVE_INFINITY;
    }
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
      super(stt.service.getServiceUrl(), "audio/x-flac", sampleRate);
      callback = stt.service.resultHandler;
      outputStream = new FLACStreamOutputStream(getOutputStream());
      encoder.setOutputStream(outputStream);
      encoder.openFLACStream();
    }

    public void finishEncoding() throws IOException
    {
      assert !stt.service.isInQueue(this);

      int availableSamples = encoder.samplesAvailableToEncode();
      if (STT.debug) {
        System.out.format(
          "%.4g seconds left to encode after recording finished.%n",
          (double) availableSamples / streamConfiguration.getSampleRate());
      }

      long encodingStartTime = STT.debug ? System.nanoTime() : 0;
      encoder.t_encodeSamples(availableSamples, true, availableSamples);
      if (STT.debug) {
        logExcessDuration(encodingStartTime, 10, TimeUnit.SECONDS,
          "Encoding the remaining samples took too long");
      }

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


  private static void logExcessDuration( long startTimeNanos, long maxDuration,
    TimeUnit maxDurationUnit, String message )
  {
    long duration = System.nanoTime() - startTimeNanos;
    if (duration >= maxDurationUnit.toNanos(maxDuration)) {
      System.err.format("%s: %.4g seconds%n", message, duration * 1e-9);
    }
  }


  private static int[] convertTo16Bit( AudioEvent ev, int[] conversionBuffer )
  {
    final float[] audioFloat = ev.getFloatBuffer();
    final int len = audioFloat.length - ev.getOverlap();
    final int[] audioInt =
      (conversionBuffer != null && len <= conversionBuffer.length) ?
        conversionBuffer :
        new int[len];
    final int offset = ev.getOverlap() / 2;
    for (int i = 0; i < len; i++) {
      assert Math.abs(audioFloat[i + offset]) <= 1;
      audioInt[i] = (int) (audioFloat[i + offset] * Short.MAX_VALUE);
    }
    return audioInt;
  }
}
