package kaleidok.javafx.beans.property.value;

import javafx.beans.value.ObservableBooleanValue;


public final class ConstantBooleanValue extends ConstantValueBase<Boolean>
  implements ObservableBooleanValue
{
  public static final ConstantBooleanValue
    TRUE = new ConstantBooleanValue(true),
    FALSE = new ConstantBooleanValue(false);


  private final boolean value;


  public static ConstantBooleanValue of( boolean value )
  {
    return value ? TRUE : FALSE;
  }


  private ConstantBooleanValue( boolean value )
  {
    this.value = value;
  }


  @Override
  public boolean get()
  {
    return value;
  }


  @Override
  public Boolean getValue()
  {
    return value;
  }
}
