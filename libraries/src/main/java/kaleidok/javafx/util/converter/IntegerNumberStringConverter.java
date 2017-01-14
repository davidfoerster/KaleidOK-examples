package kaleidok.javafx.util.converter;

import java.math.BigInteger;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;


public class IntegerNumberStringConverter
  extends CachingFormattedStringConverter<Integer, Format>
{
  public IntegerNumberStringConverter( Format format )
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
    int i = n.intValue();
    if ((n instanceof Long && i == n.longValue()) ||
      (n instanceof BigInteger && ((BigInteger) n).bitLength() < Integer.SIZE) ||
      i == n.doubleValue())
    {
      return i;
    }

    StringBuffer sb = getStringBufferInstance()
      .append("Not an integer or exceeds number range: ");
    getFormat().format(n, sb, getFieldPositionInstance());
    throw new NumberFormatException(sb.toString());
  }


  @Override
  protected ParseException getParseException( String source, ParsePosition pos )
  {
    return new ParseException("Not a valid integer", pos.getErrorIndex());
  }
}
