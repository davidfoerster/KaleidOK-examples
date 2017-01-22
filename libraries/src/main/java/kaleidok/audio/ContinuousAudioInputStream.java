package kaleidok.audio;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import org.apache.commons.io.IOUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ContinuousAudioInputStream implements ResettableAudioStream
{
  private final AudioInputStream underlying;

  private final TarsosDSPAudioFormat format;


  public ContinuousAudioInputStream( AudioInputStream source )
    throws IOException
  {
    if (!source.markSupported())
      throw new IllegalArgumentException("Source doesn’t support marks for rewinding");

    format = JVMAudioInputStream.toTarsosDSPFormat(source.getFormat());
    int frameSize = format.getFrameSize();
    long frameLength = source.getFrameLength();
    if (frameSize <= 0 || frameLength <= 0)
      throw new IllegalArgumentException("Non-positive audio data size");
    if (frameLength > Integer.MAX_VALUE / frameSize)
      throw new IOException("The audio source is too long for mark support");
    source.mark((int) frameLength * frameSize);

    underlying = source;
  }


  public ContinuousAudioInputStream( InputStream is )
    throws IOException, UnsupportedAudioFileException
  {
    this(AudioSystem.getAudioInputStream(
      is.markSupported() ?
        is :
        new ByteArrayInputStream(IOUtils.toByteArray(is))));
  }


  public ContinuousAudioInputStream( String path )
    throws IOException, UnsupportedAudioFileException
  {
    this(new BufferedInputStream(new FileInputStream(path)));
  }


  @Override
  public long skip( long bytesToSkip ) throws IOException
  {
    if (bytesToSkip <= 0)
      return 0;

    long s = bytesToSkip % (format.getFrameSize() * getFrameLength());
    long rv = underlying.skip(s);
    if (rv <= 0) {
      underlying.reset();
      rv = underlying.skip(s);
      if (rv <= 0)
        return rv;
    }
    return bytesToSkip - s + rv;
  }


  @Override
  public int read( byte[] b, int off, int len ) throws IOException
  {
    int count = underlying.read(b, off, len);
    if (count < 0) {
      underlying.reset();
      count = underlying.read(b, off, len);
    }
    return count;
  }


  @Override
  public void close() throws IOException
  {
    underlying.close();
  }


  @Override
  public TarsosDSPAudioFormat getFormat()
  {
    return format;
  }


  @Override
  public long getFrameLength()
  {
    return underlying.getFrameLength();
  }


  @Override
  public void reset() throws IOException
  {
    underlying.reset();
  }
}
