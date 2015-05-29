package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import ddf.minim.analysis.FFT;
import kaleidok.audio.spectrum.Spectrum;


public class MinimFFTProcessor implements AudioProcessor, Spectrum
{
  private FFT fft = null;

  private float sampleRate = 0;

  private final int bufferSize;

  private int avgType = 0, avgParam1, avgParam2;

  public MinimFFTProcessor( int bufferSize )
  {
    this.bufferSize = bufferSize;
  }

  public void noAverages()
  {
    avgType = 0;
    updateAverages();
  }

  public void linAverages( int bands )
  {
    avgType = 1;
    avgParam1 = bands;
    updateAverages();
  }

  public void logAverages( int minBandwidth, int bandsPerOctave )
  {
    avgType = 2;
    avgParam1 = minBandwidth;
    avgParam2 = bandsPerOctave;
    updateAverages();
  }

  private void updateAverages()
  {
    if (fft != null) {
      switch (avgType) {
      case 0:
        fft.noAverages();
        break;

      case 1:
        fft.linAverages(avgParam1);
        break;

      case 2:
        fft.logAverages(avgParam1, avgParam2);
        break;

      default:
        throw new Error("avgType has an unexpected value");
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
      fft = new FFT(bufferSize, sampleRate);
      updateAverages();
    }

    fft.forward(audioEvent.getFloatBuffer());
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
  public int getSize()
  {
    return bufferSize;
  }

  public float getSampleRate()
  {
    return sampleRate;
  }

  public float getBin( float freq )
  {
    return fft.freqToIndex(freq);
  }

  public float getFreq( float bin )
  {
    return getFreq((int) bin);
  }

  public float getFreq( int bin )
  {
    return fft.getAverageCenterFrequency(bin);
  }
}