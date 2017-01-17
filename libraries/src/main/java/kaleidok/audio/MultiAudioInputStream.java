package kaleidok.audio;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MultiAudioInputStream implements TarsosDSPAudioInputStream
{
  /**
   * No consistency checking for null values or incompatible formats is
   * performed!
   */
  public List<TarsosDSPAudioInputStream> streams;

  public MultiAudioInputStream()
  {
    this(new ArrayList<>());
  }

  public MultiAudioInputStream( List<TarsosDSPAudioInputStream> streams )
  {
    this.streams = streams;
  }


  private volatile int currentIdx = 0;

  public int getCurrentIdx()
  {
    return currentIdx;
  }

  public void setCurrentIdx( int currentIdx ) throws IOException
  {
    this.currentIdx = currentIdx;
    TarsosDSPAudioInputStream stream = getCurrentStream();
    if (stream instanceof ResettableAudioStream)
      ((ResettableAudioStream) stream).reset();
  }


  public void skipToNext( boolean cyclic ) throws IOException
  {
    int next = currentIdx + 1;
    if (cyclic) {
      next %= streams.size();
    } else if (next >= streams.size()) {
      throw new IndexOutOfBoundsException(Integer.toString(next));
    }
    setCurrentIdx(next);
  }


  public TarsosDSPAudioInputStream getCurrentStream()
  {
    return streams.get(currentIdx);
  }


  @Override
  public long skip( long bytesToSkip ) throws IOException
  {
    return getCurrentStream().skip(bytesToSkip);
  }


  @Override
  public int read( byte[] b, int off, int len ) throws IOException
  {
    return getCurrentStream().read(b, off, len);
  }


  @Override
  public void close() throws IOException
  {
    for (TarsosDSPAudioInputStream stream: streams)
    {
      if (stream != null)
        stream.close();
    }
  }


  public void clear() throws IOException
  {
    close();
    streams.clear();
  }


  @Override
  public TarsosDSPAudioFormat getFormat()
  {
    TarsosDSPAudioFormat format = getCurrentStream().getFormat();
    assert isFormatCompatible(format);
    return format;
  }


  public boolean isFormatCompatible( final TarsosDSPAudioFormat format )
  {
    return streams.stream()
      .map(TarsosDSPAudioInputStream::getFormat)
      .allMatch((streamFormat) ->
        format == streamFormat || format.matches(streamFormat));
  }


  @Override
  public long getFrameLength()
  {
    return getCurrentStream().getFrameLength();
  }
}
