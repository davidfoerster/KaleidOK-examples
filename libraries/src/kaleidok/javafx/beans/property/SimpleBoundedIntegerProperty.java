package kaleidok.javafx.beans.property;

import javafx.beans.property.SimpleIntegerProperty;


public class SimpleBoundedIntegerProperty extends SimpleIntegerProperty
  implements BoundedIntegerValue
{
  public final int min, max;


  public SimpleBoundedIntegerProperty( int initialValue, int min,
    int max )
  {
    this(null, null, initialValue, min, max);
  }


  public SimpleBoundedIntegerProperty( Object bean, String name,
    int initialValue, int min, int max )
  {
    super(bean, name, initialValue);
    this.min = min;
    this.max = max;
    checkValue(initialValue);
  }


  @Override
  public void set( int segmentCount )
  {
    super.set(checkValue(segmentCount));
  }


  private int checkValue( int segmentCount )
  {
    if (segmentCount >= min && segmentCount <= max)
    {
      return segmentCount;
    }

    throw new IllegalArgumentException(String.format(
      "%s value must be between %d and %d",
      PropertyUtils.getName(this, null), min, max));
  }


  @Override
  public int getMin()
  {
    return min;
  }


  @Override
  public int getMax()
  {
    return max;
  }
}
