package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.Property;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.prefs.Preferences;


public class StringConversionPropertyPreferencesAdapter<T, P extends Property<T>>
  extends PropertyPreferencesAdapter<T, P>
{
  public final StringConverter<T> converter;


  public StringConversionPropertyPreferencesAdapter( P property,
    StringConverter<T> converter )
  {
    super(property);
    this.converter = Objects.requireNonNull(converter);
  }


  public StringConversionPropertyPreferencesAdapter( P property, Class<?> beanClass,
    StringConverter<T> converter )
  {
    super(property, beanClass);
    this.converter = Objects.requireNonNull(converter);
  }


  public StringConversionPropertyPreferencesAdapter( P property,
    Preferences preferences, StringConverter<T> converter )
  {
    super(property, preferences);
    this.converter = Objects.requireNonNull(converter);
  }


  public StringConversionPropertyPreferencesAdapter( P property,
    String prefix, StringConverter<T> converter )
  {
    super(property, prefix);
    this.converter = Objects.requireNonNull(converter);
  }


  public StringConversionPropertyPreferencesAdapter( P property,
    Preferences preferences, String prefix, StringConverter<T> converter )
  {
    super(property, preferences, prefix);
    this.converter = Objects.requireNonNull(converter);
  }


  public static <T, P extends AspectedProperty<T>>
  StringConversionPropertyPreferencesAdapter<T, P> getInstance(
    P property )
  {
    return new StringConversionPropertyPreferencesAdapter<>(property,
      property.getAspect(StringConverterAspectTag.getInstance()));
  }


  public static <T, P extends AspectedProperty<T>>
  StringConversionPropertyPreferencesAdapter<T, P> getInstance(
    P property, Class<?> beanClass )
  {
    return new StringConversionPropertyPreferencesAdapter<>(property, beanClass,
      property.getAspect(StringConverterAspectTag.getInstance()));
  }


  public static <T, P extends AspectedProperty<T>>
  StringConversionPropertyPreferencesAdapter<T, P> getInstance(
    P property, Preferences preferences )
  {
    return new StringConversionPropertyPreferencesAdapter<>(
      property, preferences,
      property.getAspect(StringConverterAspectTag.getInstance()));
  }


  public static <T, P extends AspectedProperty<T>>
  StringConversionPropertyPreferencesAdapter<T, P> getInstance(
    P property, String prefix )
  {
    return new StringConversionPropertyPreferencesAdapter<>(
      property, prefix,
      property.getAspect(StringConverterAspectTag.getInstance()));
  }


  public static <T, P extends AspectedProperty<T>>
  StringConversionPropertyPreferencesAdapter<T, P> getInstance(
    P property, Preferences preferences, String prefix )
  {
    return new StringConversionPropertyPreferencesAdapter<>(
      property, preferences, prefix,
      property.getAspect(StringConverterAspectTag.getInstance()));
  }


  @Override
  protected boolean doLoad( @Nonnull String sValue )
  {
    T value = converter.fromString(UNESCAPE.translate(sValue));
    boolean performLoad = value != null;
    if (performLoad)
      property.setValue(value);
    return performLoad;
  }


  @Override
  protected void doSave()
  {
    put(converter.toString(property.getValue()));
  }
}
