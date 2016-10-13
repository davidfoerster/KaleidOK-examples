package kaleidok.util.prefs;

import java.util.OptionalInt;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public final class PreferenceUtils
{
  private PreferenceUtils() { }


  public static void flush( Preferences preferences )
  {
    try
    {
      preferences.flush();
    }
    catch (BackingStoreException ex)
    {
      System.err.format("Couldn't save preference node %s: %s%n",
        preferences.absolutePath(), ex.getLocalizedMessage());
    }
  }


  public static OptionalInt getInt( Preferences preferences, String key )
  {
    String value = preferences.get(key, null);
    if (value != null && !value.isEmpty()) try
    {
      return OptionalInt.of(Integer.parseInt(value));
    }
    catch (NumberFormatException ignored)
    {
      // return default
    }
    return OptionalInt.empty();
  }
}
