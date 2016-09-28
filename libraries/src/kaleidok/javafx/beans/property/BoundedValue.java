package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory;


public interface BoundedValue<T extends Number, SVF extends SpinnerValueFactory<T>>
  extends Property<Number>
{
  SVF getBounds();
}
