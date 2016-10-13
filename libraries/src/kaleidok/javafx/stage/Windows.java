package kaleidok.javafx.stage;

import javafx.stage.Window;

import java.util.prefs.Preferences;

import static kaleidok.util.prefs.PreferenceUtils.KEY_PART_DELIMITER;


public final class Windows
{
  private Windows() { }


  private static StringBuilder getGeometryKeyPrefix( CharSequence keyPrefix )
  {
    if (keyPrefix == null)
      keyPrefix = "";
    int prefixLen = keyPrefix.length();
    String infix = "geometry";
    StringBuilder key =
      new StringBuilder(prefixLen + infix.length() + 8).append(keyPrefix);
    if (prefixLen != 0 && key.charAt(prefixLen - 1) != KEY_PART_DELIMITER)
      key.append(KEY_PART_DELIMITER);
    return key.append(infix).append(KEY_PART_DELIMITER);
  }


  public static void saveGeometry( Window window, Preferences prefs,
    CharSequence keyPrefix )
  {
    double width = window.getWidth(), height = window.getHeight();
    if (width > 0 && height > 0)
    {
      StringBuilder key = getGeometryKeyPrefix(keyPrefix);
      int prefixLength = key.length();

      prefs.putDouble(key.append("width").toString(), width);
      key.setLength(prefixLength);
      prefs.putDouble(key.append("height").toString(), height);

      double left = window.getX(), top = window.getY();
      if (!Double.isNaN(left) && !Double.isNaN(top))
      {
        key.setLength(prefixLength);
        prefs.putDouble(key.append("left").toString(), left);
        key.setLength(prefixLength);
        prefs.putDouble(key.append("top").toString(), top);
      }
    }
  }


  public static boolean loadPosition( Window window, Preferences prefs,
    CharSequence keyPrefix )
  {
    StringBuilder key = getGeometryKeyPrefix(keyPrefix);
    int prefixLength = key.length();

    double left = prefs.getDouble(key.append("left").toString(), Double.NaN);
    key.setLength(prefixLength);
    double top = prefs.getDouble(key.append("top").toString(), Double.NaN);
    if (!Double.isNaN(left) && !Double.isNaN(top))
    {
      window.setX(left);
      window.setY(top);
      return true;
    }
    return false;
  }
}
