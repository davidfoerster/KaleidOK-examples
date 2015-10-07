package com.getflourish.stt2;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import javaFlacEncoder.FLACEncoder;
import javaFlacEncoder.FLACOutputStream;
import javaFlacEncoder.FLACStreamOutputStream;
import javaFlacEncoder.StreamConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static kaleidok.util.DebugManager.debug;
import static kaleidok.util.Math.clamp;


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

      if (task != null && STT.debug) {
        System.out.format(
          "Speech recording interrupted at %d of up to %s intervals after excess of the max. interval of %.3f s. %n",
          intervalSequenceCount,
          (stt.intervalSequenceCountMax > 0) ? stt.intervalSequenceCountMax : "∞",
          stt.getMaxTranscriptionInterval() * 1e-9);
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
    int n = stt.intervalSequenceCountMax;
    return (n > 0) ? n : Integer.MAX_VALUE;
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
      super(stt.service.getServiceUrl(), "audio/x-flac", sampleRate);
      callback = stt.service.resultHandler;
      outputStream = new FLACStreamOutputStream(getOutputStream());
      encoder.setOutputStream(outputStream);
      encoder.openFLACStream();
    }

    public void finishEncoding() throws IOException
    {
      assert !stt.service.isInQueue(this) :
        this + " is not in the queue of " + stt.service;

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


  private int[] convertTo16Bit( AudioEvent ev, int[] conversionBuffer )
  {
    final float[] audioFloat = ev.getFloatBuffer();
    final int len = audioFloat.length - ev.getOverlap();
    final int[] audioInt =
      (conversionBuffer != null && len <= conversionBuffer.length) ?
        conversionBuffer :
        new int[len];
    final int offset = ev.getOverlap() / 2;

    for (int i = 0; i < len; i++) {
      float sample = audioFloat[i + offset];
      if (sample < -1 || sample > +1) {
        sample = clamp(sample, -1, +1);
        if (debug >= 1)
          printSampleValueErrorMessage(ev, i);
      }
      audioInt[i] = (int)(sample * Short.MAX_VALUE);
    }

    return audioInt;
  }


  private long lastSampleOffset = -1;
  private float sampleMin = Float.NaN, sampleMax = Float.NaN;

  private void printSampleValueErrorMessage( AudioEvent ev, int idx )
  {
    final float[] samples = ev.getFloatBuffer();
    final int overlap = ev.getOverlap() / 2;
    long sampleOffset = ev.getSamplesProcessed();

    if (sampleOffset != lastSampleOffset)
    {
      assert sampleOffset > lastSampleOffset;
      float min = samples[overlap], max = min;
      final int end = samples.length - overlap;
      for (int i = overlap + 1; i < end; i++)
      {
        final float sample = samples[i];
        if (min > sample)
          min = sample;
        if (max < sample)
          max = sample;
      }
      sampleMin = min;
      sampleMax = max;
      lastSampleOffset = sampleOffset;
    }

    long sampleCount = sampleOffset + idx;
    System.err.format(
      "Encountered unexpected sample value %g at %d (%.3f s); min=%g, max=%g%n",
      samples[idx + overlap], sampleCount,
      sampleCount / (double) ev.getSampleRate(), sampleMin, sampleMax);
  }
}
