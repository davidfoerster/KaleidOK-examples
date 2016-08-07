package kaleidok.util.prefs;

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
      System.err.format("Couldn't save preferences %s: %s%n",
        preferences, ex.getLocalizedMessage());
    }
  }
}
