package kaleidok.javafx.beans.property.aspect.bounded;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public class BoundedDoubleTag
  extends BoundedValueTag<Double, DoubleSpinnerValueFactory>
{
  public static final BoundedDoubleTag INSTANCE = new BoundedDoubleTag();


  protected BoundedDoubleTag() { }


  @Override
  public DoubleSpinnerValueFactory setup( DoubleSpinnerValueFactory svf,
    AspectedReadOnlyProperty<? extends Number> property )
  {
    svf.setValue((Double) property.getValue());
    //noinspection unchecked,OverlyStrongTypeCast
    ((Property<Number>)(Property<?>) svf.valueProperty())
      .bindBidirectional((DoubleProperty) property);
    return svf;
  }


  public static DoubleSpinnerValueFactory floatBounds()
  {
    return new DoubleSpinnerValueFactory(-Float.MAX_VALUE, Float.MAX_VALUE);
  }
}
