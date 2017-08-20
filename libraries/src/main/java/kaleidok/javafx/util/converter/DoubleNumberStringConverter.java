package kaleidok.javafx.util.converter;

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;


public class DoubleNumberStringConverter
  extends CachingFormattedStringConverter<Double, Format>
{
  public DoubleNumberStringConverter( Format format )
  {
    super(format);
  }


  public DoubleNumberStringConverter()
  {
    this(NumberFormat.getNumberInstance());
  }


  @Override
  protected Double convertParseResult( Object n )
  {
    return (n instanceof Double) ?
      (Double) n :
      Double.valueOf(((Number) n).doubleValue());
  }


  @Override
  protected ParseException getParseException( String source, ParsePosition pos )
  {
    return new ParseException("Not a valid floating point number",
      pos.getErrorIndex());
  }
}
