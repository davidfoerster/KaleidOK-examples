package kaleidok.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.util.concurrent.locks.LockSupport;


/**
 * Objects of this class delay the audio dispatching as an
 * {@link be.tarsos.dsp.io.jvm.AudioPlayer} would during playback.
 */
public class DummyAudioPlayer implements AudioProcessor
{
  private long startTime = -1, startSample = -1;

  /**
   * Length of one single sample in nanoseconds
   */
  private double sampleLength = 0;

  @Override
  public boolean process( AudioEvent ev )
  {
    final long processedSamples = ev.getSamplesProcessed();
    if (startSample >= 0) {
      // all times in nanoseconds
      LockSupport.parkNanos(
        (long)((processedSamples - startSample) * sampleLength + 0.5) +
          startTime - System.nanoTime());
    } else {
      if (startTime < 0)
        startTime = System.nanoTime();
      startSample = processedSamples;
      sampleLength = 1e9 / ev.getSampleRate();
    }
    return true;
  }

  @Override
  public void processingFinished()
  {
    // nothing to do
  }


  /**
   * Use this if you want an accurate start time for this audio player and
   * pass the result to your audio dispatching thread instead of the audio
   * dispatcher itself.
   *
   * This also adds the DummyAudioPlayer to the processing chain just before
   * deferring to the AudioDispatcher. It should be the last element of the
   * processing chain.
   *
   * @param audioDispatcher  The audio dispatcher to wrap
   * @return  A wrapped audio dispatcher object
   */
  public Runnable addToDispatcher( final AudioDispatcher audioDispatcher )
  {
    return new Runnable() {
      @Override
      public void run()
      {
        audioDispatcher.addAudioProcessor(DummyAudioPlayer.this);
        startTime = System.nanoTime();
        audioDispatcher.run();
      }
    };
  }
}
