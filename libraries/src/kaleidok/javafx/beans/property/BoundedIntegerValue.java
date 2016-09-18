package kaleidok.javafx.beans.property;

import javafx.beans.value.ObservableIntegerValue;


public interface BoundedIntegerValue extends ObservableIntegerValue
{
  int getMin();

  int getMax();
}
