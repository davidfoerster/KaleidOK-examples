package kaleidok.javafx.beans.property.aspect;

import javafx.beans.binding.Bindings;
import javafx.beans.value.*;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public final class RestartRequiredTag<T>
  extends InstantiatingPropertyAspectTag<ObservableBooleanValue, T>
{
  private static final RestartRequiredTag<?> INSTANCE =
    new RestartRequiredTag<>();


  public static <T> RestartRequiredTag<T> getInstance()
  {
    //noinspection unchecked
    return (RestartRequiredTag<T>) INSTANCE;
  }


  private RestartRequiredTag() { }


  @Override
  public ObservableBooleanValue setup( ObservableBooleanValue aspect,
    AspectedReadOnlyProperty<? extends T> property )
  {
    return (aspect != null) ? aspect : setup(property);
  }


  @Override
  public ObservableBooleanValue setup(
    final AspectedReadOnlyProperty<? extends T> property )
  {
    if (property instanceof ObservableObjectValue)
    {
      return Bindings.notEqual(
        (ObservableObjectValue<?>) property, property.getValue());
    }
    if (property instanceof ObservableNumberValue)
    {
      ObservableNumberValue p2 = (ObservableNumberValue) property;
      if (p2 instanceof ObservableDoubleValue ||
        p2 instanceof ObservableFloatValue)
      {
        return Bindings.notEqual(p2, p2.doubleValue(), 0);
      }
      if (p2 instanceof ObservableIntegerValue ||
        p2 instanceof ObservableLongValue)
      {
        return Bindings.notEqual(p2, p2.longValue());
      }
    }

    throw new UnsupportedOperationException(property.getClass().getName());
  }
}
