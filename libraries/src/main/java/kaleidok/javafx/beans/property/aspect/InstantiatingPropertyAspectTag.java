package kaleidok.javafx.beans.property.aspect;

import javafx.beans.property.ReadOnlyProperty;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;


public abstract class InstantiatingPropertyAspectTag<A, T>
  extends PropertyAspectTag<A, T>
{
  public abstract A setup( AspectedReadOnlyProperty<? extends T> property );


  @Override
  public A setup( A aspect, AspectedReadOnlyProperty<? extends T> property )
  {
    if (aspect == null)
      return setup(property);

    throw new IllegalArgumentException(
      "Trying to setup an existing aspect instance from " +
        this.getClass().getName());
  }


  public A ofDefault( AspectedReadOnlyProperty<T> property )
  {
    A aspect = of(property);
    if (aspect == null)
      aspect = property.addAspect(this);
    return aspect;
  }


  public A ofDefault( ReadOnlyProperty<T> property )
  {
    return (property instanceof AspectedReadOnlyProperty) ?
      ofDefault((AspectedReadOnlyProperty<T>) property) :
      null;
  }


  @SuppressWarnings("unchecked")
  public A ofAnyDefault( AspectedReadOnlyProperty<?> property )
  {
    A aspect = ofAny(property);
    if (aspect == null)
    {
      aspect = property.addAspect(
        (InstantiatingPropertyAspectTag<A, Object>) this);
    }
    return aspect;
  }


  public A ofAnyDefault( ReadOnlyProperty<?> property )
  {
    return (property instanceof AspectedReadOnlyProperty) ?
      ofAnyDefault((AspectedReadOnlyProperty<?>) property) :
      null;
  }
}
