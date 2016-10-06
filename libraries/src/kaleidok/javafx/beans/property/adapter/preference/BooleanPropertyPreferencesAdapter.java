package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.BooleanProperty;

import java.util.prefs.Preferences;


public class BooleanPropertyPreferencesAdapter<P extends BooleanProperty>
  extends PropertyPreferencesAdapter<Boolean, P>
{
  public BooleanPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public BooleanPropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public BooleanPropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public BooleanPropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  public void load()
  {
    String value = preferences.get(key, null);
    if (value != null)
      property.set(Boolean.parseBoolean(value));
  }


  @Override
  public void save()
  {
    preferences.putBoolean(key, property.get());
  }
}
