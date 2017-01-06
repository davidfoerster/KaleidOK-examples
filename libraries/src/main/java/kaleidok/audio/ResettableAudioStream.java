package kaleidok.audio;

import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;


public interface ResettableAudioStream extends TarsosDSPAudioInputStream
{
  /**
   * @throws IOException see {@link AudioInputStream#reset()}
   */
  void reset() throws IOException;
}
