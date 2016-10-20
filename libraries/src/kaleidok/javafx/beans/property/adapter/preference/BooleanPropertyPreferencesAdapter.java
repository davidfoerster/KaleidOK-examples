package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.BooleanProperty;

import javax.annotation.Nonnull;
import java.util.prefs.Preferences;


public class BooleanPropertyPreferencesAdapter<P extends BooleanProperty>
  extends PropertyPreferencesAdapter<Boolean, P>
{
  public BooleanPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public BooleanPropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
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
  protected void doLoad( @Nonnull String value )
  {
    property.set(Boolean.parseBoolean(value));
  }


  @Override
  protected void doSave()
  {
    preferences.putBoolean(key, property.get());
  }
}
