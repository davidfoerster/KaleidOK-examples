package kaleidok.audio.spectrum;

import java.lang.Math;

import static kaleidok.util.Math.sum;


public class LinearAverageSpectrum implements Spectrum
{
  public final Spectrum spectrum;

  private double bandwidth = Double.NaN, binsPerBand = Double.NaN;

  private int N = 0;

  public LinearAverageSpectrum( Spectrum spectrum )
  {
    this.spectrum = spectrum;
  }

  public void setBands( int n )
  {
    if (this.N != n) {
      if (n <= 0)
        throw new IllegalArgumentException("n ≤ 0");

      this.N = n;
      bandwidth = Double.NaN;
      binsPerBand = Double.NaN;
      update();
    }
  }

  private void setBandwidth( double bandwidth )
  {
    if (bandwidth != this.bandwidth) {
      if (bandwidth <= 0)
        throw new IllegalArgumentException("bandwidth ≤ 0");

      this.bandwidth = bandwidth;
      binsPerBand = Double.NaN;
      N = 0;
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
        N = (int) Math.ceil(sampleRate / (bandwidth * 2));
      }
      assert N != 0;
      bandwidth = (double) sampleRate / (N * 2);
      binsPerBand = (double) spectrum.getSize() / N;
    }
    return true;
  }

  @Override
  public float get( int n )
  {
    double begin = n * binsPerBand, end = begin + binsPerBand;
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
    return N;
  }

  @Override
  public float getSampleRate()
  {
    return spectrum.getSampleRate();
  }

  @Override
  public float getBinFloat( float freq )
  {
    return freq / (float) bandwidth;
  }

  @Override
  public int getBin( float freq )
  {
    return Math.round(getBinFloat(freq));
  }

  @Override
  public float getFreq( float n )
  {
    return n * (float) bandwidth;
  }

  @Override
  public float getFreq( int n )
  {
    return getFreq((float) n);
  }
}
