package kaleidok.javafx.beans.property;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;


public final class PropertyUtils
{
  private PropertyUtils() { }


  public static String getName( ReadOnlyProperty<?> prop, String defaultName )
  {
    String name = prop.getName();
    return !name.isEmpty() ? name : getFallbackName(prop, defaultName);
  }


  public static String getName( ObservableValue<?> o, String defaultName )
  {
    return (o instanceof ReadOnlyProperty) ?
      getName((ReadOnlyProperty<?>) o, defaultName) :
      getFallbackName(o, defaultName);
  }


  private static String getFallbackName( Object o, String name )
  {
    return (name != null) ? name : o.getClass().getSimpleName();
  }
}
