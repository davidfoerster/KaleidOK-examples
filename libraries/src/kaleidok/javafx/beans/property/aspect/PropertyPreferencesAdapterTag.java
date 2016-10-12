package kaleidok.javafx.beans.property.aspect;

import kaleidok.javafx.beans.property.AspectedProperty;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;


public final class PropertyPreferencesAdapterTag<T, P extends AspectedReadOnlyProperty<T>, A extends ReadOnlyPropertyPreferencesAdapter<T, P>>
  extends InstantiatingPropertyAspectTag<A,T>
{
  private static final PropertyPreferencesAdapterTag<?,?,?> INSTANCE =
    new PropertyPreferencesAdapterTag<>();


  @SuppressWarnings("unchecked")
  public static <T, P extends AspectedReadOnlyProperty<T>, A extends ReadOnlyPropertyPreferencesAdapter<T, P>>
  PropertyPreferencesAdapterTag<T, P, A> getInstance()
  {
    return (PropertyPreferencesAdapterTag<T, P, A>) INSTANCE;
  }


  @SuppressWarnings("unchecked")
  public static <T, P extends AspectedProperty<T>, A extends PropertyPreferencesAdapter<T, P>>
  PropertyPreferencesAdapterTag<T, P, A> getWritableInstance()
  {
    return (PropertyPreferencesAdapterTag<T, P, A>) INSTANCE;
  }


  private PropertyPreferencesAdapterTag() { }


  @Override
  public A setup( A aspect, AspectedReadOnlyProperty<? extends T> property )
  {
    if (aspect == null)
    {
      aspect = setup(property);
    }
    else if (property != aspect.property)
    {
      throw new IllegalArgumentException(
        "Cannot add preference adapter of different property");
    }
    return aspect;
  }


  @SuppressWarnings("unchecked")
  @Override
  public A setup( AspectedReadOnlyProperty<? extends T> property )
  {
    return (A) PreferenceBean.ofAny((P) property);
  }
}
