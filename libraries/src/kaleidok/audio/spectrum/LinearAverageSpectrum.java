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
        throw new IllegalArgumentException("bandwidth must be positive");

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
      binsPerBand = (double) spectrum.size() / N;
    }
    return true;
  }

  @Override
  public float get( int n )
  {
    double begin = n * binsPerBand, end = begin + binsPerBand;
    assert end <= spectrum.size();
    int lowBin = (int) begin + 1, highBin = (int) end;

    double x;
    if (lowBin <= highBin) {
      x = 0;
      if (lowBin > 0)
        x += (lowBin - begin) * spectrum.get(lowBin - 1); // left side
      if (highBin < spectrum.size())
        x += (end - highBin) * spectrum.get(highBin); // right side
      x += sum(spectrum, lowBin, highBin - lowBin);
    } else {
      x = binsPerBand * spectrum.get(highBin);
    }
    return (float) x;
  }

  @Override
  public int size()
  {
    return N;
  }

  @Override
  public float getSampleRate()
  {
    return spectrum.getSampleRate();
  }

  @Override
  public float getBin( float freq )
  {
    return freq / (float) bandwidth;
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
