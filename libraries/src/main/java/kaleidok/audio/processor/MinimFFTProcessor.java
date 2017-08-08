package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import ddf.minim.analysis.FFT;
import kaleidok.audio.spectrum.Spectrum;
import org.apache.commons.lang3.ArrayUtils;


public class MinimFFTProcessor implements AudioProcessor, Spectrum
{
  public enum AverageType {
    NONE, LINEAR, LOGARITHMIC
  }


  private FFT fft = null;

  private float sampleRate = 0;

  private final float[] sampleBuffer;

  private AverageType avgType = AverageType.NONE;

  private int[] avgParams = ArrayUtils.EMPTY_INT_ARRAY;


  public MinimFFTProcessor( int bufferSize )
  {
    this.sampleBuffer = new float[bufferSize];
  }


  private void updateAverages()
  {
    if (fft != null) {
      switch (avgType) {
      case NONE:
        fft.noAverages();
        break;

      case LINEAR:
        fft.linAverages(avgParams[0]);
        break;

      case LOGARITHMIC:
        fft.logAverages(avgParams[0], avgParams[1]);
        break;
      }
    }
  }


  public void setAverageParams( AverageType type, int... params )
  {
    if (params.length != type.ordinal())
    {
      throw new IllegalArgumentException(String.format(
        "Average type %s requires exactly %d parameters, not %d",
        type.name(), type.ordinal(), params.length));
    }

    avgType = type;
    avgParams = params;
    updateAverages();
  }


  public boolean isReady()
  {
    return fft != null;
  }


  @Override
  public boolean process( AudioEvent audioEvent )
  {
    final float[] sampleBuffer = this.sampleBuffer;

    if (fft == null) {
      sampleRate = audioEvent.getSampleRate();
      fft = new FFT(sampleBuffer.length, sampleRate);
      updateAverages();
    }

    System.arraycopy(
      audioEvent.getFloatBuffer(), 0, sampleBuffer, 0, sampleBuffer.length);
    fft.forward(sampleBuffer);
    return true;
  }


  @Override
  public void processingFinished()
  {
    // Nothing to do here
  }


  @Override
  public float get( int n )
  {
    return fft.getAvg(n);
  }


  @Override
  public float[] get( final float[] a, int offset, int first, int length )
  {
    final FFT fft = this.fft;
    for (final int last = first + length; first < last; first++, offset++)
      a[offset] = fft.getAvg(first);
    return a;
  }


  @Override
  public int size()
  {
    return sampleBuffer.length;
  }


  @Override
  public float getSampleRate()
  {
    return sampleRate;
  }


  @Override
  public float getBin( float freq )
  {
    return fft.freqToIndex(freq);
  }


  @Override
  public float getFreq( float bin )
  {
    return getFreq((int) bin);
  }


  @Override
  public float getFreq( int bin )
  {
    return fft.getAverageCenterFrequency(bin);
  }
}
