package kaleidok.javafx.beans.property.aspect.bounded;

import javafx.scene.control.SpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;


public class BoundedValueTag<T extends Number, SVF extends SpinnerValueFactory<T>>
  extends PropertyAspectTag<SVF, Number>
{
  private static final BoundedValueTag<?,?> INSTANCE = new BoundedValueTag<>();


  @SuppressWarnings("unchecked")
  public static <T extends Number, SVF extends SpinnerValueFactory<T>>
  BoundedValueTag<T, SVF> getInstance()
  {
    return (BoundedValueTag<T, SVF>) INSTANCE;
  }


  protected BoundedValueTag() { }


  @Override
  public SVF setup( SVF aspect, AspectedReadOnlyProperty<? extends Number> property )
  {
    throw new UnsupportedOperationException("This tag is for look-up only");
  }


  @Override
  public final int hashCode()
  {
    return 0x06582171;
  }


  @Override
  public final boolean equals( Object obj )
  {
    return obj instanceof BoundedValueTag;
  }


  @SuppressWarnings("rawtypes")
  @Override
  protected final Class<? extends BoundedValueTag> getTagClass()
  {
    return BoundedValueTag.class;
  }
}
