package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;


public abstract class PropertyPreferencesAdapter<T, P extends Property<T>>
  extends ReadOnlyPropertyPreferencesAdapter<T, P>
{
  protected PropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  protected PropertyPreferencesAdapter( P property, Preferences preferences )
  {
    super(property, preferences);
  }

  protected PropertyPreferencesAdapter( P property, String prefix )
  {
    super(property, prefix);
  }

  protected PropertyPreferencesAdapter( P property, Preferences preferences, String prefix )
  {
    super(property, preferences, prefix);
  }


  public abstract void load();


  private volatile PreferenceChangeListener autoLoadListener = null;

  public boolean isAutoLoad()
  {
    return autoLoadListener != null;
  }

  public synchronized void setAutoLoad( boolean autoLoad )
  {
    if (autoLoad == isAutoLoad())
      return;

    if (autoLoad)
    {
      autoLoadListener = (evt) -> {
          if (evt.getNewValue() != null && key.equals(evt.getKey()))
            load();
        };
      load();
      preferences.addPreferenceChangeListener(autoLoadListener);
    }
    else
    {
      preferences.removePreferenceChangeListener(autoLoadListener);
      autoLoadListener = null;
    }
  }


  private ReadOnlyBooleanWrapper restartRequired = null;

  protected ReadOnlyBooleanProperty restartRequiredProperty()
  {
    if (restartRequired == null)
    {
      restartRequired = new ReadOnlyBooleanWrapper(
        this, "restart required", false);
    }
    return restartRequired.getReadOnlyProperty();
  }

  protected boolean isRestartRequired()
  {
    return restartRequired != null && restartRequired.get();
  }
}
