package kaleidok.audio.processor;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;


public class VolumeLevelProcessor implements AudioProcessor
{
  private double level = Double.NaN;


  public double getLevel()
  {
    return level;
  }


  @Override
  public boolean process( AudioEvent audioEvent )
  {
    float[] buf = audioEvent.getFloatBuffer();
    /*
     * We could simply use AudioEvent#getRMS() here, but it relies on a
     * numerically unstable sum of squares implementation.
     */
    level = Math.sqrt(kaleidok.util.Math.sumOfSquares(buf) / buf.length);
    return true;
  }


  @Override
  public void processingFinished()
  {
    // nothing to do here
  }
}
