package kaleidok.examples.sphinx;

public class ByteArrayInputStream extends java.io.ByteArrayInputStream
{
  public ByteArrayInputStream( byte[] buf, int offset, int length )
  {
    super(buf, offset, length);
  }

  public void reset( byte[] buf, int offset, int length )
  {
    this.buf = buf;
    this.pos = offset;
    this.count = Math.min(offset + length, buf.length);
    mark(this.count);
  }
}
