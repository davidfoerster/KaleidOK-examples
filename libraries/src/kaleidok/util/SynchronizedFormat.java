package kaleidok.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;


public class SynchronizedFormat extends Format
{
  private static final long serialVersionUID = -3031855448890951779L;

  private volatile Format underlying;

  private StringBuffer buffer = null;


  public SynchronizedFormat()
  {
    this(null);
  }


  public SynchronizedFormat( Format format )
  {
    doSetUnderlying(format);
  }


  public void setUnderlying( Format format )
  {
    doSetUnderlying(format);
  }

  private void doSetUnderlying( Format format )
  {
    this.underlying = (format != null) ? (Format) format.clone() : null;
  }


  public boolean hasUnderlying()
  {
    return underlying != null;
  }


  public synchronized String format( Object obj, FieldPosition pos )
  {
    if (buffer == null) {
      buffer = new StringBuffer();
    } else {
      buffer.setLength(0);
    }

    return format(obj, buffer, pos).toString();
  }


  @Override
  public synchronized StringBuffer format( Object obj, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    return underlying.format(obj, toAppendTo, pos);
  }


  @Override
  public synchronized Object parseObject( String source, ParsePosition pos )
  {
    return underlying.parseObject(source, pos);
  }


  @SuppressWarnings("NonAtomicOperationOnVolatileField")
  @Override
  public SynchronizedFormat clone()
  {
    SynchronizedFormat other = (SynchronizedFormat) super.clone();
    other.buffer = null;
    if (other.underlying != null)
      other.underlying = (Format) other.underlying.clone();
    return other;
  }
}
