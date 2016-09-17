package kaleidok.util.prefs;

import kaleidok.util.Reflection;
import kaleidok.util.Strings;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


public final class BeanUtils
{
  private BeanUtils() { }


  public static int applyBeanProperties( Properties prop, Package root,
    Object bean, Set<String> appliedProperties )
  {
    String prefix = removePrefix(root, bean.getClass());
    PropertySetter ps = null;
    int count = 0;

    if (!prop.isEmpty()) try
    {
      for (PropertyDescriptor pd :
        Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors())
      {
        Method wm = pd.getWriteMethod();
        if (wm != null)
        {
          ps = new PropertyDescriptorSetter(prop, prefix, pd, wm, bean);
          if (ps.tryAssignment())
          {
            count++;
            if (appliedProperties != null)
              appliedProperties.add(ps.getEntry().getKey());
          }
        }
      }

      for (Field f: bean.getClass().getFields())
      {
        ps = new FieldPropertySetter(prop, prefix, f, bean);
        if (ps.tryAssignment())
        {
          count++;
          if (appliedProperties != null)
            appliedProperties.add(ps.getEntry().getKey());
        }
      }
    }
    catch (IntrospectionException ex)
    {
      throw new AssertionError(ex);
    }
    catch (InvocationTargetException | IllegalAccessException ex)
    {
      throw new IllegalArgumentException(
        String.format(
          "Cannot set value of property \"%s.%s\" of type %s to \"%s\"",
          bean.getClass().getName(), ps.getName(), ps.getType().getName(),
          ps.getEntry().getValue()),
        (ex instanceof InvocationTargetException) ? ex.getCause() : ex);
    }

    return count;
  }


  private static String removePrefix( Package prefix, Class<?> clazz )
  {
    String
      prefixName = prefix.getName(),
      className = clazz.getCanonicalName();

    if (Strings.startsWithToken(className, prefixName, '.'))
    {
      return (className.length() != prefixName.length()) ?
        className.substring(prefixName.length() + 1) :
        null;
    }

    throw new IllegalArgumentException(
      className + " is no class of package " + prefixName +
        " or its subpackages");
  }


  private abstract static class PropertySetter
  {
    private final Properties prop;

    private final String prefix;

    protected final Object bean;


    protected PropertySetter( Properties prop, String prefix, Object bean )
    {
      this.prop = prop;
      this.prefix = prefix;
      this.bean = bean;
    }


    public abstract String getName();


    public abstract Class<?> getType();


    protected abstract boolean doAssignment( Object value )
      throws InvocationTargetException, IllegalAccessException;


    private Map.Entry<String, String> entry = null;

    private static final Map.Entry<?, ?> EMPTY_ENTRY =
      new AbstractMap.SimpleImmutableEntry<>(null, null);

    public Map.Entry<String, String> getEntry()
    {
      Map.Entry<String, String> entry = this.entry;
      if (entry == null)
      {
        String key, val;
        if (prefix != null)
        {
          key = prefix + '.' + getName();
          val = prop.getProperty(key);
          if (val == null)
          {
            key = key.substring(prefix.length());
            val = prop.getProperty(key);
          }
        }
        else
        {
          key = getName();
          val = prop.getProperty(key);
        }
        //noinspection unchecked
        this.entry = entry = (val != null) ?
          new AbstractMap.SimpleImmutableEntry<>(key, val) :
          (Map.Entry<String, String>) EMPTY_ENTRY;
      }
      return (entry != EMPTY_ENTRY) ? entry : null;
    }


    public boolean tryAssignment()
      throws InvocationTargetException, IllegalAccessException
    {
      Class <?> type = getType();
      if (CharSequence.class.isAssignableFrom(type) ||
        Reflection.getPrimitiveType(type) != null)
      {
        Map.Entry<String, String> entry = getEntry();
        if (entry != null)
        {
          return doAssignment(
            DefaultValueParser.valueOf(entry.getValue(), type));
        }
      }
      return false;
    }
  }


  private static class PropertyDescriptorSetter extends PropertySetter
  {
    private final PropertyDescriptor propertyDescriptor;

    private final Method writeMethod;


    public PropertyDescriptorSetter( Properties prop, String prefix,
      PropertyDescriptor propertyDescriptor, Method writeMethod, Object bean )
    {
      super(prop, prefix, bean);
      this.propertyDescriptor = propertyDescriptor;
      this.writeMethod = writeMethod;
    }


    @Override
    public String getName()
    {
      return propertyDescriptor.getName();
    }


    @Override
    public Class<?> getType()
    {
      return propertyDescriptor.getPropertyType();
    }


    @Override
    protected boolean doAssignment( Object value )
      throws InvocationTargetException, IllegalAccessException
    {
      writeMethod.invoke(bean, value);
      return true;
    }
  }


  private static class FieldPropertySetter extends PropertySetter
  {
    private final Field field;


    public FieldPropertySetter( Properties prop, String prefix, Field field,
      Object bean )
    {
      super(prop, prefix, bean);
      this.field = field;
    }


    @Override
    public String getName()
    {
      return field.getName();
    }


    @Override
    public Class<?> getType()
    {
      return field.getType();
    }


    @Override
    protected boolean doAssignment( Object value ) throws IllegalAccessException
    {
      field.set(bean, value);
      return true;
    }
  }
}
