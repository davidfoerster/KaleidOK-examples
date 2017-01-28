package kaleidok.processing;

import kaleidok.util.Reflection;
import processing.core.PApplet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class SimplePAppletFactory<T extends PApplet> implements PAppletFactory<T>
{
  protected final Constructor<? extends T> constructor;


  public SimplePAppletFactory( Class<? extends T> sketchClass )
    throws IllegalArgumentException
  {
    if (!PApplet.class.isAssignableFrom(sketchClass))
    {
      throw new IllegalArgumentException(new ClassCastException(
        sketchClass.getName() + " is no child class of " +
          PApplet.class.getName()));
    }
    if (Modifier.isAbstract(sketchClass.getModifiers()))
    {
      throw new IllegalArgumentException(new InstantiationException(
        sketchClass.getCanonicalName() + " is abstract"));
    }
    try
    {
      constructor = sketchClass.getConstructor(ProcessingSketchApplication.class);
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalArgumentException(ex);
    }
  }


  @Override
  public T createInstance( ProcessingSketchApplication<T> context,
    Consumer<? super T> callback, List<String> args )
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

    if (callback != null)
      callback.accept(sketch);

    String sketchName =
      Reflection.getAnonymousClassSimpleName(sketch.getClass());
    String[] extArgs;
    if (args != null && !args.isEmpty())
    {
      Stream<String> sArgs =
        args.stream().filter((s) -> !"--fullscreen".equals(s));
      extArgs =
        Stream.concat(sArgs, Stream.of(sketchName))
          .toArray(String[]::new);
    }
    else
    {
      extArgs = new String[]{ sketchName };
    }

    PApplet.runSketch(extArgs, sketch);
    return sketch;
  }
}
