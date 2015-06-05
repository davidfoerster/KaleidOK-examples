package com.getflourish.stt2;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import javaFlacEncoder.FLACEncoder;
import javaFlacEncoder.FLACOutputStream;
import javaFlacEncoder.FLACStreamOutputStream;
import javaFlacEncoder.StreamConfiguration;

import java.io.IOException;


class AudioTranscriptionProcessor implements AudioProcessor
{
  private STT stt;

  private StreamConfiguration streamConfiguration = null;

  private FLACEncoder encoder = new FLACEncoder();

  private final TranscriptionThread thread;

  private FlacTranscriptionTask task = null;

  private int[] audioConversionBuffer = null;

  public boolean shouldRecord = false;

  private boolean debug = false;

  protected AudioTranscriptionProcessor( STT stt, TranscriptionThread transcriptionThread )
  {
    this.stt = stt;
    this.thread = transcriptionThread;
    transcriptionThread.start();
  }

  public void setDebug( boolean debug )
  {
    this.debug = debug;
    thread.debug = debug;
  }

  public void setLanguage( String language )
  {
    thread.language = language;
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
      } catch (IOException ex) {
        ex.printStackTrace();
        if (task != null) {
          task.finalizeTask();
          task = null;
        }
        stt.end(false);
      }
    } else {
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

    if (task == null) {
      task = new FlacTranscriptionTask(ev.getSampleRate());
    }
  }

  private void finishEncoding()
  {
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


  private class FlacTranscriptionTask extends TranscriptionTask
  {
    private final FLACOutputStream outputStream;

    public FlacTranscriptionTask( float sampleRate ) throws IOException
    {
      super(thread, "audio/x-flac", sampleRate);
      outputStream = new FLACStreamOutputStream(getOutputStream());
      encoder.setOutputStream(outputStream);
      encoder.openFLACStream();
    }

    public void finishEncoding() throws IOException
    {
      assert !isScheduled();
      int availableSamples = encoder.samplesAvailableToEncode();
      if (debug) {
        System.out.format(
          "%.4g seconds left to encode after recording finished.%n",
          (double) availableSamples / streamConfiguration.getSampleRate());
      }

      long now = debug ? System.nanoTime() : 0;
      encoder.t_encodeSamples(availableSamples, true, availableSamples);
      if (debug)
        assertShorter(now, 10 * 1000L*1000L, "Encoding remaining samples took too long");
      schedule();
    }

    @Override
    public void finalizeTask()
    {
      try {
        outputStream.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        super.finalizeTask();
      }
    }
  }


  private static boolean assertShorter( long startTime, long maxDuration, String message )
  {
    long duration = System.nanoTime() - startTime;
    if (duration < maxDuration)
      return true;

    System.err.format("%s: %.4g seconds%n", message, duration * 1e-9);
    return false;
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
