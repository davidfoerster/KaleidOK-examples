package kaleidok.javafx.beans.property.aspect.bounded;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public class BoundedDoubleTag<SVF extends SpinnerValueFactory<Double>>
  extends BoundedValueTag<Double, SVF>
{
  private static final BoundedDoubleTag<?> INSTANCE = new BoundedDoubleTag<>();


  @SuppressWarnings("unchecked")
  public static <SVF extends SpinnerValueFactory<Double>>
  BoundedDoubleTag<SVF> getDoubleInstance()
  {
    return (BoundedDoubleTag<SVF>) INSTANCE;
  }


  protected BoundedDoubleTag() { }


  @Override
  public SVF setup( SVF svf,
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
