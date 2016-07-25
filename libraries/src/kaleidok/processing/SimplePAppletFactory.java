package kaleidok.processing;

import processing.core.PApplet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;


public class SimplePAppletFactory<T extends PApplet> implements PAppletFactory<T>
{
  protected final Constructor<? extends T> constructor;


  public static <T extends PApplet> SimplePAppletFactory<T> forClass(
    Class<T> appletClass )
    throws IllegalArgumentException
  {
    return new SimplePAppletFactory<>(appletClass);
  }


  public SimplePAppletFactory( Class<? extends T> appletClass )
    throws IllegalArgumentException
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
      constructor = appletClass.getConstructor(ProcessingSketchApplication.class);
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalArgumentException(ex);
    }
  }


  @Override
  public T createInstance( ProcessingSketchApplication<T> context,
    List<String> args )
    throws InvocationTargetException
  {
    Objects.requireNonNull(args);

    T sketch;
    try
    {
      sketch = constructor.newInstance(context);
    }
    catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }

    String[] extArgs = args.toArray(new String[args.size() + 1]);
    extArgs[extArgs.length - 1] = sketch.getClass().getSimpleName();
    PApplet.runSketch(extArgs, sketch);
    return sketch;
  }
}
