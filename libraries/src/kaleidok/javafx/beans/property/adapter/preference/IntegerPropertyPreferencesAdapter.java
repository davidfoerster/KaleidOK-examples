package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.IntegerProperty;

import javax.annotation.Nonnull;
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
  protected void doLoad( @Nonnull String value )
  {
    property.set(Integer.parseInt(value));
  }


  @Override
  protected void doSave()
  {
    preferences.putInt(key, property.get());
  }
}
