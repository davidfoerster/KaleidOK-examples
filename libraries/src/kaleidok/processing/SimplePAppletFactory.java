package kaleidok.processing;

import processing.core.PApplet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;


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
    if (!PApplet.class.isAssignableFrom(appletClass))
    {
      throw new IllegalArgumentException(new ClassCastException(
        appletClass.getName() + " is no child class of " +
          PApplet.class.getName()));
    }
    if (Modifier.isAbstract(appletClass.getModifiers()))
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
    T sketch;
    try
    {
      sketch = constructor.newInstance(context);
    }
    catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex)
    {
      throw new AssertionError(ex);
    }

    String[] extArgs =
      (args != null && !args.isEmpty()) ?
        args.stream()
          .filter((s) -> !"--fullscreen".equals(s))
          .toArray((l) -> new String[l + 1]) :
        new String[1];
    extArgs[extArgs.length - 1] = sketch.getClass().getSimpleName();
    PApplet.runSketch(extArgs, sketch);
    return sketch;
  }
}
