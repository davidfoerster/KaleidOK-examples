package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.IntegerProperty;

import java.util.prefs.Preferences;


public class IntegerPropertyPreferencesAdapter<P extends IntegerProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public IntegerPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public IntegerPropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
  }

  public IntegerPropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public IntegerPropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public IntegerPropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  public void load()
  {
    String value = preferences.get(key, null);
    if (value != null)
      property.set(Integer.parseInt(value));
  }


  @Override
  public void save()
  {
    preferences.putInt(key, property.get());
  }
}
