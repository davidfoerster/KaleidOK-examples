package kaleidok.javafx.beans.property.value;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.Objects;


public abstract class ConstantValueBase<T>
  implements ObservableValue<T>, Cloneable
{
  @Override
  public boolean equals( Object obj )
  {
    return obj == this ||
      (obj instanceof ConstantValueBase &&
         Objects.equals(this.getValue(), ((ConstantValueBase<?>) obj).getValue()));
  }


  @Override
  public int hashCode()
  {
    return Objects.hashCode(getValue()) ^ 0x0c7c840e;
  }


  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    if (sb.length() == 0)
      sb.append(getClass().getName());
    valueToString(sb.append('['));
    return sb.append(']').toString();
  }


  protected void valueToString( StringBuilder sb )
  {
    sb.append(getValue());
  }


  @Override
  public ConstantValueBase<T> clone()
  {
    try
    {
      //noinspection unchecked
      return (ConstantValueBase<T>) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }
  }


  @Override
  public void addListener( ChangeListener<? super T> listener ) { }


  @Override
  public void removeListener( ChangeListener<? super T> listener ) { }


  @Override
  public void addListener( InvalidationListener listener ) { }


  @Override
  public void removeListener( InvalidationListener listener ) { }
}
