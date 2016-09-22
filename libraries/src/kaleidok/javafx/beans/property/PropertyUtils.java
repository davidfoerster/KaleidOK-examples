package kaleidok.javafx.beans.property;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import kaleidok.util.Strings;
import kaleidok.util.function.InstanceSupplier;
import kaleidok.util.prefs.DefaultValueParser;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    return (name != null) ? name : o.getClass().getSimpleName();
  }


  private static final String PROPERTY_METHOD_SUFFIX = "Property";


  public static Set<Property<Object>> getProperties( final Object bean,
    Set<Property<Object>> resultSet )
  {
    @SuppressWarnings("unchecked")
    Stream<Property<Object>> fieldProps =
      Stream.of(bean.getClass().getFields())
        .filter(( f ) ->
          !Modifier.isStatic(f.getModifiers()) &&
            Property.class.isAssignableFrom(f.getType()))
        .map(( f ) -> {
            try {
              return (Property<Object>) f.get(bean);
            } catch (IllegalAccessException ex) {
              throw new AssertionError(ex);
            }
          });

    @SuppressWarnings("unchecked")
    Stream<Property<Object>> methodProps =
      Stream.of(bean.getClass().getMethods())
        .filter(( m ) ->
          !Modifier.isStatic(m.getModifiers()) &&
            m.getParameterCount() == 0 &&
            Property.class.isAssignableFrom(m.getReturnType()) &&
            m.getName().endsWith(PROPERTY_METHOD_SUFFIX))
        .map(( m ) -> {
            try {
              return (Property<Object>) m.invoke(bean, EMPTY_OBJECT_ARRAY);
            } catch (ReflectiveOperationException ex) {
              throw new AssertionError(ex);
            }
          });

    Stream<Property<?>> allProps =
      Stream.concat(fieldProps, methodProps).filter(Objects::nonNull);

    return (resultSet != null) ?
      allProps.sequential().collect(
        Collectors.toCollection(new InstanceSupplier<>((resultSet)))) :
      allProps.collect(Collectors.toSet());
  }


  public static int applyProperties( final Map<String, String> src,
    final String root, Set<? extends Property<?>> dst,
    Collection<String> appliedProperties )
    throws IllegalArgumentException
  {
    int appliedCount = 0;
    for (Property<?> prop: dst)
    {
      String srcKey = applyProperty(src, root, prop);

      if (srcKey != null)
      {
        appliedCount++;
        if (appliedProperties != null)
          appliedProperties.add(srcKey);
      }
    }
    return appliedCount;
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


  public static void applyProperty( final Property<?> prop, String strValue )
    throws IllegalArgumentException, UnsupportedOperationException
  {
    Class<?> propType = getPropertyValueType(prop);
    if (propType == null)
    {
      throw new UnsupportedOperationException(
        "Unsupported property type " + prop.getClass().getName());
    }

    try
    {
      //noinspection unchecked
      ((WritableValue<Object>) prop).setValue(
        DefaultValueParser.valueOf(strValue, propType));
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
  public static <T> Class<? extends T> getPropertyValueType(
    WritableObjectValue<T> prop )
  {
    return (Class<? extends T>) (
      (prop instanceof WritableStringValue) ? String.class :
      (prop instanceof WritableListValue) ? ObservableList.class :
      (prop instanceof WritableSetValue) ? ObservableSet.class :
      (prop instanceof WritableMapValue) ? ObservableMap.class :
        null);
  }
}
