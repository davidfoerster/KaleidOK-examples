package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import kaleidok.util.Reflection;
import kaleidok.util.Strings;
import kaleidok.util.prefs.DefaultValueParser;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;


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
    return (name != null) ?
      name :
      Reflection.getAnonymousClassSimpleName(o.getClass());
  }


  private static final String PROPERTY_METHOD_SUFFIX = "Property";


  public static Stream<Property<?>> getProperties( final Object bean )
  {
    @SuppressWarnings("unchecked")
    Stream<Property<?>> fieldProps =
      Stream.of(bean.getClass().getFields())
        .filter(( f ) ->
          (f.getModifiers() & (STATIC | FINAL)) == 0 &&
            Property.class.isAssignableFrom(f.getType()))
        .map(( f ) -> {
            try {
              return (Property<?>) f.get(bean);
            } catch (IllegalAccessException ex) {
              throw new AssertionError(ex);
            }
          });

    @SuppressWarnings("unchecked")
    Stream<Property<?>> methodProps =
      Stream.of(bean.getClass().getMethods())
        .filter(( m ) ->
          !Modifier.isStatic(m.getModifiers()) &&
            m.getParameterCount() == 0 &&
            Property.class.isAssignableFrom(m.getReturnType()) &&
            m.getName().endsWith(PROPERTY_METHOD_SUFFIX))
        .map(( m ) -> {
            try {
              return (Property<?>) m.invoke(bean, EMPTY_OBJECT_ARRAY);
            } catch (ReflectiveOperationException ex) {
              throw new AssertionError(ex);
            }
          });

    return
      Stream.concat(fieldProps, methodProps)
        .filter(Objects::nonNull)
        .distinct();
  }


  public static Stream<String> applyProperties( final Map<String, String> src,
    final String root, Stream<? extends Property<?>> dst )
    throws IllegalArgumentException
  {
    return dst
      .map((prop) -> applyProperty(src, root, prop))
      .filter(Objects::nonNull);
  }


  public static String applyProperty( Map<String, String> src, String root,
    Property<?> dst )
    throws IllegalArgumentException
  {
    if (root != null)
    {
      if (root.isEmpty())
        throw new IllegalArgumentException("Empty root");

      assert Package.getPackage(root) != null :
        "Non-existing package: " + root;
    }

    Object bean = dst.getBean();
    CharSequence shortName = dst.getName();
    if (bean != null && shortName.length() != 0)
    {
      shortName = Strings.toCamelCase(shortName);
      String fullName =
        bean.getClass().getCanonicalName() + '.' + shortName;
      String srcKey = fullName;
      String strValue = src.get(srcKey);

      if (strValue == null)
      {
        if (root != null && Strings.startsWithToken(fullName, root, '.'))
        {
          srcKey = fullName.substring(root.length() + 1);
          strValue = src.get(srcKey);
        }

        if (strValue == null)
        {
          srcKey = fullName.substring(
            fullName.length() - shortName.length() - 1);  // = "." + shortName;
          strValue = src.get(srcKey);
        }
      }

      if (strValue != null) try
      {
        applyProperty(dst, strValue);
        return srcKey;
      }
      catch (IllegalArgumentException | UnsupportedOperationException ex)
      {
        throw new IllegalArgumentException(
          String.format(
            "Cannot assign property %s from key %s",
            fullName, srcKey),
          ex);
      }
    }

    return null;
  }


  public static <T> void applyProperty( final Property<T> prop, String strValue )
    throws IllegalArgumentException, UnsupportedOperationException
  {
    Class<? extends T> propType = getPropertyValueType(prop);
    if (propType == null)
    {
      throw new UnsupportedOperationException(
        "Unsupported property type " + prop.getClass().getName());
    }

    try
    {
      prop.setValue(DefaultValueParser.valueOf(strValue, propType));
    }
    catch (IllegalArgumentException ex)
    {
      throw new IllegalArgumentException(
        String.format(
          "Cannot assign property of type %s from \"%s\"",
          propType.getName(), strValue),
        ex);
    }
  }


  @SuppressWarnings("unchecked")
  public static <T> Class<? extends T> getPropertyValueType(
    WritableValue<T> prop )
  {
    return (Class<? extends T>) (
      (prop instanceof WritableNumberValue) ?
        getPropertyValueType((WritableNumberValue) prop) :
      (prop instanceof WritableObjectValue) ?
        getPropertyValueType((WritableObjectValue<T>) prop) :
      (prop instanceof WritableBooleanValue) ?
        Boolean.class :
        null);
  }


  public static Class<? extends Number> getPropertyValueType(
    WritableNumberValue prop )
  {
    return
      (prop instanceof WritableIntegerValue) ? Integer.class :
      (prop instanceof WritableLongValue) ? Long.class :
      (prop instanceof WritableFloatValue) ? Float.class :
      (prop instanceof WritableDoubleValue) ? Double.class :
        Number.class;
  }


  @SuppressWarnings("unchecked")
  public static <T> Class<T> getPropertyValueType(
    WritableObjectValue<T> prop )
  {
    return (Class<T>) (
      (prop instanceof WritableStringValue) ? String.class :
      (prop instanceof WritableListValue) ? ObservableList.class :
      (prop instanceof WritableSetValue) ? ObservableSet.class :
      (prop instanceof WritableMapValue) ? ObservableMap.class :
        null);
  }


  public static <T> void debugPropertyChanges( ReadOnlyProperty<T> property )
  {
    property.addListener(( observable, oldValue, newValue ) -> {
      if (!Objects.equals(oldValue, newValue))
      {
        //noinspection unchecked
        System.out.format(
          "Property \"%s\": \"%s\" -> \"%s\"%n",
          StringUtils.defaultIfEmpty(
            ((ReadOnlyProperty<T>) observable).getName(), "<unnamed>"),
          oldValue, newValue);
      }
    });
  }
}
