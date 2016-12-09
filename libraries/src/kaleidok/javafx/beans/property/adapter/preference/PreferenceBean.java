package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.*;
import javafx.beans.value.ObservableNumberValue;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;

import java.util.stream.Stream;


public interface PreferenceBean
{
  String getName();


  Object getParent();


  Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>> getPreferenceAdapters();


  static Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters( Stream<? extends PreferenceBean> prefBeans )
  {
    return prefBeans.flatMap(PreferenceBean::getPreferenceAdapters);
  }


  default void saveAndFlush()
  {
    ReadOnlyPropertyPreferencesAdapter.saveAndFlush(getPreferenceAdapters());
  }


  static <P extends Property<String>> StringPropertyPreferencesAdapter<P>
  of( P stringProperty )
  {
    return new StringPropertyPreferencesAdapter<>(stringProperty);
  }


  static <P extends IntegerProperty> IntegerPropertyPreferencesAdapter<P>
  of( P integerProperty )
  {
    return new IntegerPropertyPreferencesAdapter<>(integerProperty);
  }


  static <P extends LongProperty> LongPropertyPreferencesAdapter<P>
  of( P longProperty )
  {
    return new LongPropertyPreferencesAdapter<>(longProperty);
  }


  static <P extends FloatProperty> FloatPropertyPreferencesAdapter<P>
  of( P floatProperty )
  {
    return new FloatPropertyPreferencesAdapter<>(floatProperty);
  }


  static <P extends DoubleProperty> DoublePropertyPreferencesAdapter<P> of( P doubleProperty )
  {
    return new DoublePropertyPreferencesAdapter<>(doubleProperty);
  }


  static <P extends BooleanProperty> BooleanPropertyPreferencesAdapter<P>
  of( P booleanProperty )
  {
    return new BooleanPropertyPreferencesAdapter<>(booleanProperty);
  }


  static <T, P extends Property<T>>
  PropertyPreferencesAdapter<T, P> ofAny( P property )
  {
    PropertyPreferencesAdapter<?, ?> ppa =
      (property instanceof ObservableNumberValue) ? (
        (property instanceof IntegerProperty) ?
          new IntegerPropertyPreferencesAdapter<>((IntegerProperty) property) :
        (property instanceof DoubleProperty) ?
          new DoublePropertyPreferencesAdapter<>((DoubleProperty) property) :
        (property instanceof LongProperty) ?
          new LongPropertyPreferencesAdapter<>((LongProperty) property) :
        (property instanceof FloatProperty) ?
          new FloatPropertyPreferencesAdapter<>((FloatProperty) property) :
          null) :
      (property instanceof StringProperty) ?
        new StringPropertyPreferencesAdapter<>((StringProperty) property) :
      (property instanceof BooleanProperty) ?
        new BooleanPropertyPreferencesAdapter<>((BooleanProperty) property) :
        null;

    if (ppa == null)
    {
      if (property instanceof AspectedReadOnlyProperty)
      {
        @SuppressWarnings("unchecked")
        StringConverter<T> converter =
          ((AspectedReadOnlyProperty<T>) property).getAspect(
            StringConverterAspectTag.getInstance());
        if (converter != null)
        {
          ppa =
            new StringConversionPropertyPreferencesAdapter<>(
              property, converter);
        }
      }
    }

    if (ppa != null)
    {
      //noinspection unchecked
      return (PropertyPreferencesAdapter<T, P>) ppa;
    }

    throw new UnsupportedOperationException(
      "Unsupported property type: " + property.getClass().getName());
  }


  static <T, P extends ReadOnlyProperty<T>>
  ReadOnlyPropertyPreferencesAdapter<T, P> ofAny( P property )
  {
    if (property instanceof Property)
    {
      //noinspection unchecked
      return (ReadOnlyPropertyPreferencesAdapter<T, P>) ofAny((Property<T>) property);
    }

    /*
    if (property instanceof ObservableNumberValue)
    {
      if (property instanceof ReadOnlyIntegerProperty)
        return new IntegerPropertyPreferencesAdapter((ReadOnlyIntegerProperty) property);
      if (property instanceof ReadOnlyDoubleProperty)
        return new DoublePropertyPreferencesAdapter((ReadOnlyDoubleProperty) property);
      if (property instanceof ReadOnlyLongProperty)
        return new LongPropertyPreferencesAdapter((ReadOnlyLongProperty) property);
      if (property instanceof ReadOnlyFloatProperty)
        return new FloatPropertyPreferencesAdapter((ReadOnlyFloatProperty) property);
    }
    else
    {
      if (property instanceof ReadOnlyStringProperty)
        //noinspection OverlyStrongTypeCast
        return new StringPropertyPreferencesAdapter((ReadOnlyStringProperty) property);
      if (property instanceof ReadOnlyBooleanProperty)
        return new BooleanPropertyPreferencesAdapter((ReadOnlyBooleanProperty) property);
    }
    */

    throw new UnsupportedOperationException(
      "Unsupported property type: " + property.getClass().getName());
  }
}
