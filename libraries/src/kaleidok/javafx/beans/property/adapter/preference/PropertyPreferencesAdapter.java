package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.property.Property;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import static kaleidok.util.logging.LoggingUtils.logThrown;


public abstract class PropertyPreferencesAdapter<T, P extends Property<T>>
  extends ReadOnlyPropertyPreferencesAdapter<T, P>
{
  protected PropertyPreferencesAdapter( P property )
  {
    super(property);
  }

  protected PropertyPreferencesAdapter( P property, Class<?> beanClass )
  {
    super(property, beanClass);
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


  public boolean load()
  {
    boolean loadPerformed = false;

    String value = preferences.get(key, null);
    if (value != null)
    {
      try
      {
        loadPerformed = doLoad(value);
      }
      catch (RuntimeException ex)
      {
        if (logger != null)
        {
          logThrown(logger, Level.WARNING,
            "Error loading preference value {0}/{1}: {2}", ex,
            new Object[]{ preferences.absolutePath(), key, value });
        }
        value = null;
      }
    }

    if (logger != null && logger.isLoggable(LOG_LEVEL))
    {
      logger.log(LOG_LEVEL,
        (value != null) ?
          "Loaded preference value {0}/{1} = \"{2}\"" :
          "Loaded preference value {0}/{1} but there was no valid entry",
        new Object[]{ preferences.absolutePath(), key, property.getValue() });
    }

    return loadPerformed;
  }


  protected abstract boolean doLoad( @Nonnull String value );


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
}
