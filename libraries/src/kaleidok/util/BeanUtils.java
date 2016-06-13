package kaleidok.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;


public final class BeanUtils
{
  private BeanUtils() { }


  public static int applyBeanProperties( Properties prop, Package root, Object o )
  {
    String prefix = removePrefix(root, o.getClass());
    String name = null;
    Map.Entry<String, Object> entry = null;
    int count = 0;

    if (!prop.isEmpty()) try
    {
      for (PropertyDescriptor pd :
        Introspector.getBeanInfo(o.getClass()).getPropertyDescriptors())
      {
        if (pd.getWriteMethod() != null &&
          (pd.getPropertyType().isPrimitive() ||
            CharSequence.class.isAssignableFrom(pd.getPropertyType())))
        {
          name = pd.getName();
          entry = lookupValue(prop, prefix, name);
          if (entry != null) {
            assert entry.getValue() instanceof CharSequence;
            pd.getWriteMethod().invoke(o,
              DefaultValueParser.valueOf(
                entry.getValue().toString(), pd.getPropertyType()));
            count++;
          }
        }
      }

      for (Field f: o.getClass().getFields())
      {
        name = f.getName();
        entry = lookupValue(prop, prefix, name);
        if (entry != null) {
          assert entry.getValue() instanceof CharSequence;
          f.set(o,
            DefaultValueParser.valueOf(
              entry.getValue().toString(), f.getType()));
          count++;
        }
      }

    } catch (IntrospectionException ex) {
      throw new AssertionError(ex);
    } catch (InvocationTargetException | IllegalAccessException ex) {
      throw new IllegalArgumentException(
        String.format(
          "Cannot set value of property \"%s.%s\" to \"%s\"",
          o.getClass().getName(), name, entry.getValue()),
        (ex instanceof InvocationTargetException) ? ex.getCause() : ex);
    }

    return count;
  }


  private static String removePrefix( Package prefix, Class<?> clazz )
  {
    String
      prefixName = prefix.getName(),
      className = clazz.getCanonicalName();

    if (className.startsWith(prefixName)) {
      if (className.length() == prefixName.length()) {
        return null;
      } else if (className.charAt(prefixName.length()) == '.') {
        return className.substring(prefixName.length() + 1);
      }
    }

    throw new IllegalArgumentException(
      className + " is no class of package " + prefixName +
        " or its subpackages");
  }


  private static Map.Entry<String, Object> lookupValue( Properties prop,
    String prefix, String name )
  {
    String key;
    Object val;
    if (prefix != null) {
      key = prefix + '.' + name;
      val = prop.get(key);
      if (val == null) {
        key = key.substring(prefix.length());
        val = prop.get(key);
      }
    } else {
      key = name;
      val = prop.get(key);
    }
    return (val != null) ? new AbstractMap.SimpleEntry<>(key, val) : null;
  }
}
