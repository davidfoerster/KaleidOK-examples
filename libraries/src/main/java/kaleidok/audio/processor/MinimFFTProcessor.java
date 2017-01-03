package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import ddf.minim.analysis.FFT;
import kaleidok.audio.spectrum.Spectrum;


public class MinimFFTProcessor implements AudioProcessor, Spectrum
{
  private enum AverageType {
    NONE, LINEAR, LOGARITHMIC
  }


  private FFT fft = null;

  private float sampleRate = 0;

  private final float[] sampleBuffer;

  private AverageType avgType = AverageType.NONE;

  private int avgParam1, avgParam2;


  public MinimFFTProcessor( int bufferSize )
  {
    this.sampleBuffer = new float[bufferSize];
  }


  public void noAverages()
  {
    avgType = AverageType.NONE;
    updateAverages();
  }


  public void linAverages( int bands )
  {
    avgType = AverageType.LINEAR;
    avgParam1 = bands;
    updateAverages();
  }


  public void logAverages( int minBandwidth, int bandsPerOctave )
  {
    avgType = AverageType.LOGARITHMIC;
    avgParam1 = minBandwidth;
    avgParam2 = bandsPerOctave;
    updateAverages();
  }


  private void updateAverages()
  {
    if (fft != null) {
      switch (avgType) {
      case NONE:
        fft.noAverages();
        break;

      case LINEAR:
        fft.linAverages(avgParam1);
        break;

      case LOGARITHMIC:
        fft.logAverages(avgParam1, avgParam2);
        break;
      }
    }
  }


  public boolean isReady()
  {
    return fft != null;
  }


  @Override
  public boolean process( AudioEvent audioEvent )
  {
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
  public float[] get( float[] a, int offset, int first, int length )
  {
    for (int last = first + length; first < last; first++, offset++)
      a[offset] = get(first);
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
