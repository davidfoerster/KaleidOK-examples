package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;


public final class ConverterUtil
{
  public static final StringConverter<Integer> INTEGER_STRING_CONVERTER =
    new IntegerStringConverter();

  public static final StringConverter<Float> FLOAT_STRING_CONVERTER =
    new FloatStringConverter();

  public static final StringConverter<Double> DOUBLE_STRING_CONVERTER =
    new DoubleStringConverter();

  private ConverterUtil() { }
}
