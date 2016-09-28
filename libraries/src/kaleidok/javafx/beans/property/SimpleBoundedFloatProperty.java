package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.binding.BidirectionalNumberBindingBase;
import kaleidok.javafx.util.converter.ConverterUtil;
import kaleidok.javafx.util.converter.StringConvertible;


public class SimpleBoundedFloatProperty extends SimpleFloatProperty
  implements BoundedValue<Double, DoubleSpinnerValueFactory>,
  StringConvertible<Float>
{
  private final DoubleSpinnerValueFactory svf;


  public SimpleBoundedFloatProperty( float initialValue, float min,
    float max )
  {
    this(null, null, initialValue, min, max);
  }


  public SimpleBoundedFloatProperty( Object bean, String name )
  {
    this(bean, name, 0);
  }


  public SimpleBoundedFloatProperty( Object bean, String name,
    float initialValue )
  {
    this(bean, name, initialValue, -Float.MAX_VALUE, Float.MAX_VALUE);
  }


  public SimpleBoundedFloatProperty( Object bean, String name,
    float initialValue, float min, float max )
  {
    this(bean, name, initialValue, min, max, 1);
  }


  public SimpleBoundedFloatProperty( Object bean, String name,
    float initialValue, float min, float max, float step )
  {
    super(bean, name, initialValue);
    svf = new DoubleSpinnerValueFactory(
      min, max, checkValue(initialValue, min, max), step);

    ChangeListener<Number> binding =
      new DoubleBidirectionalNumberBinding(svf.valueProperty(), this);
    svf.valueProperty().addListener(binding);
    this.addListener(binding);
  }


  @Override
  public void set( float value )
  {
    super.set(checkValue(value));
  }


  private float checkValue( float value )
  {
    return checkValue(value, svf.getMin(), svf.getMax());
  }

  private float checkValue( float value, double min, double max )
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


  @Override
  public StringConverter<Float> getStringConverter()
  {
    return ConverterUtil.FLOAT_STRING_CONVERTER;
  }


  private static class DoubleBidirectionalNumberBinding
    extends BidirectionalNumberBindingBase<Double>
  {
    public DoubleBidirectionalNumberBinding( Property<Double> p1,
      Property<Number> p2 )
    {
      super(p1, p2);
    }


    @Override
    protected Double convertValue( Number v )
    {
      return v.doubleValue();
    }
  }
}
