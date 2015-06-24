package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;
import kaleidok.audio.spectrum.Spectrum;


public class FFTProcessor implements AudioProcessor, Spectrum
{
  private float sampleRate = Float.NaN;

  public final float[] amplitudes;

  public final FFT fft;

  private final float[] transformBuffer;

  public FFTProcessor( int bufferSize )
  {
    amplitudes = new float[bufferSize / 2];
    transformBuffer = new float[bufferSize];
    fft = new FFT(bufferSize);
  }

  @Override
  public boolean process( AudioEvent audioEvent )
  {
    sampleRate = audioEvent.getSampleRate();
    float[] audioFloatBuffer = audioEvent.getFloatBuffer();
    System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length);
    fft.forwardTransform(transformBuffer);
    fft.modulus(transformBuffer, amplitudes);
    return true;
  }

  @Override
  public void processingFinished()
  {
    // Nothing to do here
  }

  @Override
  public float get( int bin )
  {
    return amplitudes[bin];
  }

  @Override
  public int size()
  {
    return amplitudes.length;
  }

  public float getSampleRate()
  {
    return sampleRate;
  }

  public float getBin( float freq )
  {
    return freq / sampleRate * (amplitudes.length * 2);
  }

  public float getFreq( float bin )
  {
    return bin / (amplitudes.length * 2) * sampleRate;
  }

  public float getFreq( int bin )
  {
    return getFreq((float) bin);
  }
}
