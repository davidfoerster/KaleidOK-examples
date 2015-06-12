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
  private final STT stt;

  private StreamConfiguration streamConfiguration = null;

  private FLACEncoder encoder = new FLACEncoder();

  private FlacTranscription task = null;

  private int[] audioConversionBuffer = null;

  public volatile boolean shouldRecord = false;

  protected AudioTranscriptionProcessor( STT stt, TranscriptionService service )
  {
    this.stt = stt;
    new Thread(service, "Speech transcription").start();
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
          task.dispose();
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
      task = new FlacTranscription(ev.getSampleRate());
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
      assert !stt.service.executor.contains(this);

      int availableSamples = encoder.samplesAvailableToEncode();
      if (STT.debug) {
        System.out.format(
          "%.4g seconds left to encode after recording finished.%n",
          (double) availableSamples / streamConfiguration.getSampleRate());
      }

      long now = STT.debug ? System.nanoTime() : 0;
      encoder.t_encodeSamples(availableSamples, true, availableSamples);
      if (STT.debug)
        assertShorter(now, 10 * 1000L*1000L, "Encoding remaining samples took too long");

      stt.service.add(this);
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
