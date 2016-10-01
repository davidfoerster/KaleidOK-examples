package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Objects;


public abstract class NumberStringConverterBase<T extends Number>
  extends StringConverter<T>
{
  private NumberFormat format;


  protected NumberStringConverterBase( NumberFormat format )
  {
    this.format = Objects.requireNonNull(format);
  }


  @Override
  public String toString( Number value )
  {
    return getFormat()
      .format(value, getStringBufferInstance(), getFieldPositionInstance())
      .toString();
  }


  @Override
  public T fromString( String string )
    throws NumberFormatException
  {
    ParsePosition parsePosition = getParsePositionInstance();
    Number result = getFormat().parse(string, parsePosition);
    if (parsePosition.getIndex() != 0)
      return convertNumber(result);

    throw new NumberFormatException(string);
  }


  protected abstract T convertNumber( Number n );


  public NumberFormat getFormat()
  {
    return format;
  }

  public void setFormat( NumberFormat format )
  {
    this.format = Objects.requireNonNull(format);
  }


  private StringBuffer cachedStringBuffer;

  protected StringBuffer getStringBufferInstance()
  {
    StringBuffer cachedStringBuffer = this.cachedStringBuffer;
    if (cachedStringBuffer == null)
    {
      this.cachedStringBuffer = cachedStringBuffer = new StringBuffer();
    }
    else
    {
      cachedStringBuffer.setLength(0);
    }
    return cachedStringBuffer;
  }


  private FieldPosition cachedFieldPosition;

  protected FieldPosition getFieldPositionInstance()
  {
    FieldPosition cachedFieldPosition = this.cachedFieldPosition;
    if (cachedFieldPosition == null)
    {
      this.cachedFieldPosition = cachedFieldPosition = new FieldPosition(0);
    }
    else
    {
      cachedFieldPosition.setBeginIndex(0);
      cachedFieldPosition.setEndIndex(0);
    }
    return cachedFieldPosition;
  }


  private ParsePosition cachedParsePosition;

  protected ParsePosition getParsePositionInstance()
  {
    ParsePosition cachedParsePosition = this.cachedParsePosition;
    if (cachedParsePosition == null)
    {
      this.cachedParsePosition = cachedParsePosition = new ParsePosition(0);
    }
    else
    {
      cachedParsePosition.setIndex(0);
      cachedParsePosition.setErrorIndex(-1);
    }
    return cachedParsePosition;
  }


  public static class DoubleNumberStringConverter
    extends NumberStringConverterBase<Double>
  {
    public DoubleNumberStringConverter( NumberFormat format )
    {
      super(format);
    }


    @Override
    protected Double convertNumber( Number n )
    {
      return (n instanceof Double) ? (Double) n : n.doubleValue();
    }
  }


  public static class IntegerNumberStringConverter
    extends NumberStringConverterBase<Integer>
  {
    public IntegerNumberStringConverter( NumberFormat format )
    {
      super(format);
    }


    @Override
    protected Integer convertNumber( Number n )
    {
      return (n instanceof Integer) ? (Integer) n : n.intValue();
    }
  }
}
