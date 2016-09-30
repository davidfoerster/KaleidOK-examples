package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;
import kaleidok.javafx.util.converter.StringConvertible;


public interface BoundedValue<T extends Number, SVF extends SpinnerValueFactory<T>>
  extends Property<Number>, StringConvertible<T>
{
  SVF getBounds();


  @Override
  default StringConverter<T> getStringConverter()
  {
    return getBounds().getConverter();
  }
}
