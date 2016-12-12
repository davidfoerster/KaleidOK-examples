package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.LongProperty;

import javax.annotation.Nonnull;
import java.util.prefs.Preferences;


public class LongPropertyPreferencesAdapter<P extends LongProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public LongPropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public LongPropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
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
  protected boolean doLoad( @Nonnull String value )
  {
    property.set(Long.parseLong(value));
    return true;
  }


  @Override
  protected void doSave()
  {
    preferences.putLong(key, property.get());
  }
}
