package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.DoubleProperty;

import javax.annotation.Nonnull;
import java.util.prefs.Preferences;


public class DoublePropertyPreferencesAdapter<P extends DoubleProperty>
  extends PropertyPreferencesAdapter<Number, P>
{
  public DoublePropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  public DoublePropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
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
  protected void doLoad( @Nonnull String value )
  {
    property.set(Double.parseDouble(value));
  }


  @Override
  protected void doSave()
  {
    preferences.putDouble(key, property.get());
  }
}
