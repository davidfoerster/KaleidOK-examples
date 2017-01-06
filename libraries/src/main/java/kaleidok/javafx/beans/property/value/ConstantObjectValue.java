package kaleidok.javafx.beans.property.value;

import javafx.beans.value.ObservableObjectValue;


public class ConstantObjectValue<T> extends ConstantValueBase<T>
  implements ObservableObjectValue<T>
{
  private static final ConstantObjectValue<?> EMPTY =
    new ConstantObjectValue<>(null);


  private final T value;


  @SuppressWarnings("unchecked")
  public static <T> ConstantObjectValue<T> empty()
  {
    return (ConstantObjectValue<T>) EMPTY;
  }


  public static <T> ConstantObjectValue<T> of( T value )
  {
    return (value != null) ? new ConstantObjectValue<>(value) : empty();
  }


  protected ConstantObjectValue( T value )
  {
    this.value = value;
  }


  @Override
  public T get()
  {
    return value;
  }


  @Override
  public T getValue()
  {
    return value;
  }
}
