package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;


public class SimpleBoundedDoubleProperty extends SimpleDoubleProperty
  implements BoundedValue<Double, DoubleSpinnerValueFactory>
{
  private final DoubleSpinnerValueFactory svf;


  public SimpleBoundedDoubleProperty( double initialValue, double min,
    double max )
  {
    this(null, null, initialValue, min, max);
  }


  public SimpleBoundedDoubleProperty( Object bean, String name )
  {
    this(bean, name, 0);
  }


  public static SimpleBoundedDoubleProperty forFloat( Object bean, String name )
  {
    return forFloat(bean, name, 0);
  }


  public SimpleBoundedDoubleProperty( Object bean, String name,
    float initialValue )
  {
    this(bean, name, initialValue, -Double.MAX_VALUE, Double.MAX_VALUE);
  }


  public static SimpleBoundedDoubleProperty forFloat( Object bean, String name,
    double initialValue )
  {
    return new SimpleBoundedDoubleProperty(
      bean, name, initialValue, -Float.MAX_VALUE, Float.MAX_VALUE);
  }


  public SimpleBoundedDoubleProperty( Object bean, String name,
    double initialValue, double min, double max )
  {
    this(bean, name, initialValue, min, max, 1);
  }


  public SimpleBoundedDoubleProperty( Object bean, String name,
    double initialValue, double min, double max, double step )
  {
    super(bean, name, initialValue);
    svf = new DoubleSpinnerValueFactory(
      min, max, checkValue(initialValue, min, max), step);

    //noinspection unchecked
    ((Property<Number>)(Property<?>) svf.valueProperty())
      .bindBidirectional(this);
  }


  @Override
  public void set( double value )
  {
    super.set(checkValue(value));
  }


  private double checkValue( double value )
  {
    return checkValue(value, svf.getMin(), svf.getMax());
  }

  private double checkValue( double value, double min, double max )
  {
    if (value >= min && value <= max)
      return value;

    throw new IllegalArgumentException(String.format(
      "%s value must be between %f and %f",
      PropertyUtils.getName(this, null), min, max));
  }


  @Override
  public DoubleSpinnerValueFactory getBounds()
  {
    return svf;
  }
}
