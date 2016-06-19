package kaleidok.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Objects of this class delay the audio dispatching as an
 * {@link be.tarsos.dsp.io.jvm.AudioPlayer} would during playback.
 */
public class DummyAudioPlayer implements AudioProcessor
{
  private static final Logger logger =
    Logger.getLogger(DummyAudioPlayer.class.getCanonicalName());

  private long startTime = -1, startSample = -1;

  /**
   * Length of one single sample in nanoseconds
   */
  private double sampleLength = 0;

  private void reset()
  {
    startTime = -1;
    startSample = -1;
    sampleLength = 0;
  }

  private void init( AudioEvent ev )
  {
    if (startTime < 0)
      startTime = System.nanoTime();
    startSample = ev.getSamplesProcessed();
    sampleLength = 1e9 / ev.getSampleRate();
  }

  @Override
  public boolean process( AudioEvent ev )
  {
    if (startSample >= 0) {
      // all times in nanoseconds
      final long delay =
        (long)((ev.getSamplesProcessed() - startSample) * sampleLength) +
          startTime - System.nanoTime();
      if (delay >= 0) {
        LockSupport.parkNanos(delay);
      } else {
        logger.log(Level.FINER,
          "Audio processing too slow by {0,number,0.0000E0} milliseconds",
          delay * -1e-6);
      }
    } else {
      init(ev);
    }
    return true;
  }

  @Override
  public void processingFinished()
  {
    reset();
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
  public Runnable addToDispatcher( final AudioDispatcher audioDispatcher,
    Runnable chained )
  {
    final Runnable _chained = (chained != null) ? chained : audioDispatcher;
    return () -> {
        audioDispatcher.addAudioProcessor(DummyAudioPlayer.this);
        startTime = System.nanoTime();
        _chained.run();
      };
  }
}
