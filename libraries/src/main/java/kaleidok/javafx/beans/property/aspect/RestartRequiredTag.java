package kaleidok.javafx.beans.property.aspect;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;

import java.util.Objects;


public final class RestartRequiredTag<T>
  extends InstantiatingPropertyAspectTag<RestartRequiredTag.RestartRequiredBinding<T>, T>
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
  public RestartRequiredBinding<T> setup( RestartRequiredBinding<T> aspect,
    AspectedReadOnlyProperty<? extends T> property )
  {
    return (aspect != null) ? aspect : setup(property);
  }


  @Override
  public RestartRequiredBinding<T> setup(
    final AspectedReadOnlyProperty<? extends T> property )
  {
    return new RestartRequiredBinding<>(property);
  }


  public static class RestartRequiredBinding<T> extends BooleanBinding
  {
    private final ObservableValue<? extends T> observable;

    private final ObjectProperty<T> referenceValue;


    public RestartRequiredBinding( ObservableValue<? extends T> observable )
    {
      this.observable = observable;
      referenceValue =
        new SimpleObjectProperty<>(this,
          "reference value for restart-required binding",
          observable.getValue());

      bind(observable, referenceValue);
    }


    @Override
    public void dispose()
    {
      unbind(observable, referenceValue);
    }


    public ObjectProperty<T> referenceValueProperty()
    {
      return referenceValue;
    }

    public T getReferenceValue()
    {
      return referenceValue.get();
    }

    public void setReferenceValue( T referenceValue )
    {
      this.referenceValue.unbind();
      this.referenceValue.set(referenceValue);
    }


    public void disarm()
    {
      this.referenceValue.bind(observable);
    }


    public T updateReferenceValue()
    {
      T value = observable.getValue();
      setReferenceValue(value);
      return value;
    }


    @Override
    protected boolean computeValue()
    {
      return !Objects.equals(referenceValue.getValue(), observable.getValue());
    }
  }
}
