package kaleidok.javafx.beans.property.aspect;

import javafx.beans.value.ObservableBooleanValue;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.value.ConstantBooleanValue;


public class HiddenAspectTag<T> extends InstantiatingPropertyAspectTag<ObservableBooleanValue, T>
{
  private static final HiddenAspectTag<?> INSTANCE = new HiddenAspectTag<>();


  @SuppressWarnings("unchecked")
  public static <T> HiddenAspectTag<T> getInstance()
  {
    return (HiddenAspectTag<T>) INSTANCE;
  }


  protected HiddenAspectTag() { }


  @Override
  public ObservableBooleanValue setup(
    AspectedReadOnlyProperty<? extends T> property )
  {
    return ConstantBooleanValue.FALSE;
  }


  @Override
  public ObservableBooleanValue setup( ObservableBooleanValue aspect,
    AspectedReadOnlyProperty<? extends T> property )
  {
    return (aspect != null) ? aspect : setup(property);
  }
}
