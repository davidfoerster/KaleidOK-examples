package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.FloatProperty;

import javax.annotation.Nonnull;
import java.util.prefs.Preferences;


public class FloatPropertyPreferencesAdapter<P extends FloatProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public FloatPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public FloatPropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
  }

  public FloatPropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public FloatPropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public FloatPropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  protected void doLoad( @Nonnull String value )
  {
    property.set(Float.parseFloat(value));
  }


  @Override
  protected void doSave()
  {
    preferences.putFloat(key, property.get());
  }
}
