package kaleidok.audio.spectrum;


import static java.lang.Math.*;
import static kaleidok.util.Math.*;


/**
 * This is broken at the moment. Don't use it!
 */
public class LogarithmicAverageSpectrum implements Spectrum
{
  public final Spectrum spectrum;

  private int N = 0, bandsPerOctave = 0;

  private double minFreq = Double.NaN, maxFreq = Double.NaN;

  /**
   * cached constant: log_2(minFreq)
   */
  private double log2_minFreq;

  /**
   * cached constant: 2^(1/bandsPerOctave)
   */
  private double c1;

  /**
   * cached constant: minFreq * bandsPerOctave / ln(2)
   */
  private double c2;

  /**
   * cached constant: c2 * (c1 - 1)
   */
  private double c3;


  public LogarithmicAverageSpectrum( Spectrum spectrum )
  {
    this.spectrum = spectrum;
  }

  public void setParameters( double minFreq, int octaves, int bandsPerOctave )
  {
    if (minFreq <= 0)
      throw new IllegalArgumentException("minFreq must be positive");
    if (octaves <= 0 || bandsPerOctave <= 0)
      throw new IllegalArgumentException("octaves and bands per octave must be positive");

    double maxFreq = scalb(minFreq, octaves);
    if (maxFreq > getSampleRate() / 2f)
      throw new IllegalArgumentException("(minFreq * 2^octaves) exceeds Nyquist frequency");

    this.minFreq = minFreq;
    this.maxFreq = maxFreq;
    this.N = octaves * bandsPerOctave;
    this.bandsPerOctave = bandsPerOctave;
    log2_minFreq = log2(minFreq);
    c1 = pow(2, 1d / bandsPerOctave);
    c2 = minFreq * bandsPerOctave * LN2_INV;
    c3 = c2 * (c1 - 1);
  }

  @Override
  public float get( int n )
  {
    assert maxFreq <= getSampleRate() / 2f;

    // TODO: work in progress
    double lowFreq = getFreq((double) n), highFreq = getFreq((double)(n + 1));
    double begin = spectrum.getBin((float) lowFreq), end = spectrum.getBin((float) highFreq);
    int lowBin = (int) begin + 1, highBin = (int) end;

    double x;
    if (lowBin <= highBin) {
      x = 0;
      if (lowBin > 0)
        x += spectrum.get(lowBin - 1) / getWeightOfBinRange(begin, lowBin); // left side
      if (highBin < spectrum.getSize())
        x += spectrum.get(highBin) / getWeightOfBinRange(highBin, end); // right side
      for (int i = lowBin; i < highBin; i++) {
        x += spectrum.get(i) * getWeightOfBin(i);
      }
    } else {
      x = spectrum.get(highBin) / getWeightOfBinRange(begin, end);
    }
    x /= (highFreq - lowFreq);
    //System.out.format("%5.0f–%5.0f Hz: %.2e\n", lowFreq, highFreq, x);
    return (float) x;
  }

  private double getWeightOfBinRange( double begin, double end )
  {
    assert begin < end;
    // surface area below exponential function
    return c2 * (pow(c1, end) - pow(c1, begin));
  }

  private double getWeightOfBin( int i )
  {
    // surface area below exponential function
    return c3 * pow(c1, i);
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

  public double getBin( double freq )
  {
    return (log2(freq) - log2_minFreq) / bandsPerOctave;
  }

  @Override
  public float getBin( float freq )
  {
    return (float) getBin((double) freq);
  }

  public double getFreq( int octave, int band )
  {
    return getFreq((double)(octave * bandsPerOctave + band));
  }

  public double getFreq( double n )
  {
    return minFreq * pow(c1, n);
  }

  @Override
  public float getFreq( float n )
  {
    return (float) getFreq((double) n);
  }

  @Override
  public float getFreq( int n )
  {
    return (float) getFreq((double) n);
  }
}
