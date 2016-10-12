package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;


public interface AspectedProperty<T>
  extends AspectedReadOnlyProperty<T>, Property<T>
{
}
