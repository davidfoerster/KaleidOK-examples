package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.LongProperty;

import java.util.prefs.Preferences;


public class LongPropertyPreferencesAdapter<P extends LongProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public LongPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public LongPropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public LongPropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public LongPropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  public void load()
  {
    String value = preferences.get(key, null);
    if (value != null)
      property.set(Long.parseLong(value));
  }


  @Override
  public void save()
  {
    preferences.putLong(key, property.get());
  }
}
