package kaleidok.javafx.beans.property.aspect.bounded;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public class BoundedIntegerTag
  extends BoundedValueTag<Integer, IntegerSpinnerValueFactory>
{
  public static final BoundedIntegerTag INSTANCE = new BoundedIntegerTag();


  protected BoundedIntegerTag() { }


  @Override
  public IntegerSpinnerValueFactory setup( IntegerSpinnerValueFactory svf,
    AspectedReadOnlyProperty<? extends Number> property )
  {
    svf.setValue((Integer) property.getValue());
    //noinspection unchecked,OverlyStrongTypeCast
    ((Property<Number>)(Property<?>) svf.valueProperty())
      .bindBidirectional((IntegerProperty) property);
    return svf;
  }
}
