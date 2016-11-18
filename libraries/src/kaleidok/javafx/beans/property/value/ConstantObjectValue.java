package kaleidok.javafx.beans.property.value;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;

import java.util.Objects;


public class ConstantObjectValue<T>
  implements ObservableObjectValue<T>, Cloneable
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


  @Override
  public void addListener( InvalidationListener listener ) { }

  @Override
  public void removeListener( InvalidationListener listener ) { }

  @Override
  public void addListener( ChangeListener<? super T> listener ) { }

  @Override
  public void removeListener( ChangeListener<? super T> listener ) { }


  @Override
  public int hashCode()
  {
    return Objects.hashCode(value) ^ 0x0c7c840e;
  }


  @Override
  public boolean equals( Object obj )
  {
    return obj == this ||
      (obj instanceof ConstantObjectValue &&
        Objects.equals(this.value, ((ConstantObjectValue<?>) obj).value));
  }


  @Override
  public String toString()
  {
    return ConstantObjectValue.class.getSimpleName() + '[' + value + ']';
  }


  @Override
  public ConstantObjectValue<T> clone()
  {
    try
    {
      //noinspection unchecked
      return (ConstantObjectValue<T>) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }
  }
}
