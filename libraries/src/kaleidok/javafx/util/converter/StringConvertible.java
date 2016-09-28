package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;


public interface StringConvertible<T>
{
  StringConverter<T> getStringConverter();
}
