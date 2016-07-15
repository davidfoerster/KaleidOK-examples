package kaleidok.processing;

import javax.swing.JApplet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;


public class SimplePAppletFactory<T extends ExtPApplet> implements PAppletFactory<T>
{
  protected final Constructor<? extends T> constructor;


  public SimplePAppletFactory( Class<? extends T> appletClass )
  {
    if (!ExtPApplet.class.isAssignableFrom(appletClass))
    {
      throw new IllegalArgumentException(new ClassCastException(
        appletClass.getName() + " is no sub-class of " +
          ExtPApplet.class.getCanonicalName()));
    }
    if ((appletClass.getModifiers() & Modifier.ABSTRACT) != 0)
    {
      throw new IllegalArgumentException(new InstantiationException(
        appletClass.getCanonicalName() + " is abstract"));
    }
    try
    {
      constructor = appletClass.getConstructor(JApplet.class);
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalArgumentException(ex);
    }
  }


  @Override
  public T createInstance( JApplet parent ) throws InvocationTargetException
  {
    try
    {
      return constructor.newInstance(parent);
    }
    catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }
  }
}
