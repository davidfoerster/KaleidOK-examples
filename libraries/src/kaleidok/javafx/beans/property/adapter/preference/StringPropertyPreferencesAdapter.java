package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.Property;

import java.util.prefs.Preferences;


public class StringPropertyPreferencesAdapter<P extends Property<String>>
  extends PropertyPreferencesAdapter<String, P>
{
  public StringPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public StringPropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public StringPropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public StringPropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  public void load()
  {
    String value = preferences.get(key, null);
    if (value != null)
      property.setValue(value);
  }


  @Override
  public void save()
  {
    String value = property.getValue();
    if (value != null)
    {
      preferences.put(key, value);
    }
    else
    {
      preferences.remove(key);
    }
  }
}
