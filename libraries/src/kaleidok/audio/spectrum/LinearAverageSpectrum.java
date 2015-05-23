package kaleidok.audio.spectrum;

import java.lang.Math;

import static kaleidok.util.Math.sum;


public class LinearAverageSpectrum implements Spectrum
{
  public final Spectrum spectrum;

  private double bandwidth = Double.NaN, binsPerBand = Double.NaN;

  private int n = 0;

  public LinearAverageSpectrum( Spectrum spectrum )
  {
    this.spectrum = spectrum;
  }

  public void setBands( int n )
  {
    if (this.n != n) {
      this.n = n;
      bandwidth = Double.NaN;
      binsPerBand = Double.NaN;
      update();
    }
  }

  private void setBandwidth( double bandwidth )
  {
    if (bandwidth != this.bandwidth) {
      this.bandwidth = bandwidth;
      binsPerBand = Double.NaN;
      n = 0;
      update();
    }
  }

  public double getBandwidth()
  {
    return bandwidth;
  }

  public boolean update()
  {
    float sampleRate = spectrum.getSampleRate();
    if (Float.isNaN(sampleRate))
      return false;

    if (Double.isNaN(binsPerBand)) {
      if (!Double.isNaN(bandwidth)) {
        n = (int) Math.ceil(sampleRate / (bandwidth * 2));
      }
      assert n != 0;
      bandwidth = (double) sampleRate / (n * 2);
      binsPerBand = (double) spectrum.getSize() / n;
    }
    return true;
  }

  @Override
  public float get( int bin )
  {
    double begin = bin * binsPerBand, end = begin + binsPerBand;
    assert end <= spectrum.getSize();
    int lowBin = (int) begin, highBin = (int) end;

    double x = sum(spectrum, lowBin, highBin - lowBin);
    double cutoff = (begin - lowBin) * spectrum.get(lowBin); // left side
    if (highBin < spectrum.getSize())
      cutoff += (end - highBin) * spectrum.get(highBin);
    return (float)(x - cutoff);
  }

  @Override
  public int getSize()
  {
    return n;
  }

  @Override
  public float getSampleRate()
  {
    return spectrum.getSampleRate();
  }

  @Override
  public float getBinFloat( float freq )
  {
    return freq / getSampleRate() * (n * 2);
  }

  @Override
  public int getBin( float freq )
  {
    return Math.round(getBinFloat(freq));
  }

  @Override
  public float getFreq( float bin )
  {
    return bin / (n * 2) * getSampleRate();
  }

  @Override
  public float getFreq( int bin )
  {
    return getFreq((float) bin);
  }
}
