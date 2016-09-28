package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.util.StringConverter;
import kaleidok.javafx.util.converter.ConverterUtil;
import kaleidok.javafx.util.converter.StringConvertible;


public class SimpleBoundedIntegerProperty extends SimpleIntegerProperty
  implements BoundedValue<Integer, IntegerSpinnerValueFactory>,
  StringConvertible<Integer>
{
  private final IntegerSpinnerValueFactory svf;


  public SimpleBoundedIntegerProperty( int initialValue, int min,
    int max )
  {
    this(null, null, initialValue, min, max);
  }


  public SimpleBoundedIntegerProperty( Object bean, String name )
  {
    this(bean, name, 0);
  }


  public SimpleBoundedIntegerProperty( Object bean, String name,
    int initialValue )
  {
    this(bean, name, initialValue, Integer.MAX_VALUE, Integer.MIN_VALUE );
  }


  public SimpleBoundedIntegerProperty( Object bean, String name,
    int initialValue, int min, int max )
  {
    this(bean, name, initialValue, min, max, 1);
  }


  public SimpleBoundedIntegerProperty( Object bean, String name,
    int initialValue, int min, int max, int step )
  {
    super(bean, name, initialValue);
    svf = new IntegerSpinnerValueFactory(
      min, max, checkValue(initialValue, min, max), step);

    //noinspection unchecked
    ((Property<Number>)(Property<?>) svf.valueProperty())
      .bindBidirectional(this);
  }


  @Override
  public void set( int value )
  {
    super.set(checkValue(value));
  }


  private int checkValue( int value )
  {
    return checkValue(value, svf.getMin(), svf.getMax());
  }

  private int checkValue( int value, int min, int max )
  {
    if (value >= min && value <= max)
      return value;

    throw new IllegalArgumentException(String.format(
      "%s value must be between %d and %d",
      PropertyUtils.getName(this, null), min, max));
  }


  @Override
  public IntegerSpinnerValueFactory getBounds()
  {
    return svf;
  }


  @Override
  public StringConverter<Integer> getStringConverter()
  {
    return ConverterUtil.INTEGER_STRING_CONVERTER;
  }
}
