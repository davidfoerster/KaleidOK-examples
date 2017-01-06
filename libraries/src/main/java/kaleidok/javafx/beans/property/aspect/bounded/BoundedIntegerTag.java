package kaleidok.javafx.beans.property.aspect.bounded;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public class BoundedIntegerTag<SVF extends SpinnerValueFactory<Integer>>
  extends BoundedValueTag<Integer, SVF>
{
  private static final BoundedIntegerTag<?> INSTANCE = new BoundedIntegerTag<>();


  @SuppressWarnings("unchecked")
  public static <SVF extends SpinnerValueFactory<Integer>>
  BoundedIntegerTag<SVF> getIntegerInstance()
  {
    return (BoundedIntegerTag<SVF>) INSTANCE;
  }


  protected BoundedIntegerTag() { }


  @Override
  public SVF setup( SVF svf,
    AspectedReadOnlyProperty<? extends Number> property )
  {
    svf.setValue((Integer) property.getValue());
    //noinspection unchecked,OverlyStrongTypeCast
    ((Property<Number>)(Property<?>) svf.valueProperty())
      .bindBidirectional((IntegerProperty) property);
    return svf;
  }
}
