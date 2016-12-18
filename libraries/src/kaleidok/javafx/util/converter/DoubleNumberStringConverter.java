package kaleidok.javafx.util.converter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;


public class DoubleNumberStringConverter
  extends CachingFormattedStringConverter<Double, NumberFormat>
{
  public DoubleNumberStringConverter( NumberFormat format )
  {
    super(format);
  }


  @Override
  protected Double convertParseResult( Object n )
  {
    return (n instanceof Double) ? (Double) n : ((Number) n).doubleValue();
  }


  @Override
  protected ParseException getParseException( String source, ParsePosition pos )
  {
    return new ParseException("Not a valid floating point number",
      pos.getErrorIndex());
  }
}
