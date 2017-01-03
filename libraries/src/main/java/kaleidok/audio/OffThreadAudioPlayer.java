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

    @SuppressWarnings("resource")
    final SourceDataLine line =
      (SourceDataLine) AudioSystem.getLine(
        new DataLine.Info(SourceDataLine.class, format, bufferSize));
    line.open();

    Pipe pipe = Pipe.open();
    this.out = pipe.sink();
    @SuppressWarnings("resource")
    final ReadableByteChannel in = pipe.source();

    offThread =
      new Thread(() -> runAudioPlayer(in, line),
        OffThreadAudioPlayer.class.getSimpleName());
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


  private static void runAudioPlayer( final ReadableByteChannel in,
    final SourceDataLine line )
  {
    final byte[] aBuf = new byte[line.getBufferSize()];
    final ByteBuffer bBuf = ByteBuffer.wrap(aBuf);

    line.start();
    try
    {
      try
      {
        int read;
        while ((read = in.read(bBuf)) >= 0)
        {
          int written = 0;
          while (written < read)
          {
            int n = line.write(aBuf, written, read - written);
            if (n <= 0)
            {
              throw new AssertionError(
                line.getClass().getName() +
                  "#write(...) returned a non-positive value " + n);
            }
            written += n;
          }
          bBuf.position(0);
        }
        line.drain();
        line.stop();
      }
      finally
      {
        in.close();
      }
    }
    catch (IOException ex)
    {
      handleUncaught(ex);
    }
    finally
    {
      line.close();
    }
  }
}
