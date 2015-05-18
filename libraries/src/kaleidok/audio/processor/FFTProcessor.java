package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;


public class FFTProcessor implements AudioProcessor
{
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
}
