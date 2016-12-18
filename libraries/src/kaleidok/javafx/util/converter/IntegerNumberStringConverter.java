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
    if (parseResult instanceof Integer)
      return (Integer) parseResult;

    Number n = (Number) parseResult;
    long l = n.longValue();
    if ((l != 0) ? (l == (int) l) : !Double.isNaN(n.doubleValue()))
      return (int) l;

    StringBuffer sb = getStringBufferInstance()
      .append("Exceeds number range: ");
    getFormat().format(n, sb, getFieldPositionInstance());
    throw new NumberFormatException(sb.toString());
  }


  @Override
  protected ParseException getParseException( String source, ParsePosition pos )
  {
    return new ParseException("Not a valid integer", pos.getErrorIndex());
  }
}
