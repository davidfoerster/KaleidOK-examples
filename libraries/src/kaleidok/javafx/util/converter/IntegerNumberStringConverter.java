package kaleidok.javafx.util.converter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;


public class IntegerNumberStringConverter
  extends CachingFormattedStringConverter<Integer, NumberFormat>
{
  public IntegerNumberStringConverter( NumberFormat format )
  {
    super(format);
  }


  public IntegerNumberStringConverter()
  {
    this(NumberFormat.getIntegerInstance());
  }


  @Override
  protected Integer convertParseResult( Object parseResult )
  {
    Number n = (Number) parseResult;
    if (parseResult instanceof Integer)
      return (Integer) parseResult;

    if (!Double.isNaN(n.doubleValue()))
    {
      long l = n.longValue();
      if (l == (int) l)
        return (int) l;
    }

    StringBuffer sb = getStringBufferInstance();
    sb.setLength(0);
    sb.append("Exceeds number range: ");
    getFormat().format(n, sb, getFieldPositionInstance());
    throw new NumberFormatException(sb.toString());
  }


  @Override
  protected Throwable getParseException( String source, ParsePosition pos )
  {
    return new ParseException("Not a valid integer", pos.getErrorIndex());
  }
}
