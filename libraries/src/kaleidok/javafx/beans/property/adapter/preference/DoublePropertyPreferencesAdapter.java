package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.DoubleProperty;

import java.util.prefs.Preferences;


public class DoublePropertyPreferencesAdapter<P extends DoubleProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public DoublePropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public DoublePropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  public DoublePropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  public DoublePropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  @Override
  public void load()
  {
    String value = preferences.get(key, null);
    if (value != null)
      property.set(Double.parseDouble(value));
  }


  @Override
  public void save()
  {
    preferences.putDouble(key, property.get());
  }
}
