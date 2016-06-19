package kaleidok.audio;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static kaleidok.util.Threads.handleUncaught;


public class OffThreadAudioPlayer implements AudioProcessor
{
  public final Thread offThread;

  private final AudioFormat format;

  private final WritableByteChannel out;


  public OffThreadAudioPlayer( AudioFormat format, int bufferSize )
    throws IOException, LineUnavailableException
  {
    this.format = format;

    Pipe pipe = Pipe.open();
    this.out = pipe.sink();

    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, bufferSize);
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
    line.open();

    offThread = new AudioPlayerThread(pipe.source(), line);
  }


  private ByteBuffer outBuf = null;

  @Override
  public boolean process( AudioEvent audioEvent )
  {
    byte[] aBuf = audioEvent.getByteBuffer();
    //noinspection ArrayEquality
    if (outBuf == null || outBuf.array() != aBuf) {
      outBuf = ByteBuffer.wrap(aBuf);
    }
    outBuf.position(audioEvent.getOverlap() * format.getFrameSize());

    try {
      out.write(outBuf);
    } catch (IOException ex) {
      handleUncaught(ex);
    }
    return true;
  }


  @Override
  public void processingFinished()
  {
    outBuf = null;
    try {
      out.close();
    } catch (IOException ex) {
      handleUncaught(ex);
    }
  }


  private static class AudioPlayerThread extends Thread
  {
    private final SourceDataLine line;

    private final ReadableByteChannel in;


    public AudioPlayerThread( ReadableByteChannel in, SourceDataLine line )
    {
      super(OffThreadAudioPlayer.class.getSimpleName());
      this.line = line;
      this.in = in;
    }


    @Override
    public void run()
    {
      final ReadableByteChannel in = this.in;
      final SourceDataLine line = this.line;
      final byte[] aBuf = new byte[line.getBufferSize()];
      final ByteBuffer bBuf = ByteBuffer.wrap(aBuf);

      line.start();
      try {
        int read;
        while ((read = in.read(bBuf)) >= 0) {
          int written = 0;
          while (written < read) {
            int n = line.write(aBuf, written, read);
            assert n > 0;
            written += n;
          }
          bBuf.position(0);
        }
      } catch (IOException ex) {
        handleUncaught(ex);
      } finally {
        line.drain();
        line.stop();
        line.close();
        try {
          in.close();
        } catch (IOException ex) {
          handleUncaught(ex);
        }
      }
    }
  }
}
