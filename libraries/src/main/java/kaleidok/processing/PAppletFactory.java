package kaleidok.processing;

import kaleidok.util.Arrays;
import processing.core.PApplet;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;


public interface PAppletFactory<T extends PApplet>
{
  T createInstance( ProcessingSketchApplication<T> context,
    Consumer<T> callback, List<String> args )
    throws InvocationTargetException;


  default T createInstance( ProcessingSketchApplication<T> context,
    Consumer<T> callback, String... args )
    throws InvocationTargetException
  {
    return createInstance(context, callback, Arrays.asImmutableList(args));
  }
}
