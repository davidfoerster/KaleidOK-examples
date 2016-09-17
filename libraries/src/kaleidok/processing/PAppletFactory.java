package kaleidok.processing;

import kaleidok.util.Arrays;
import processing.core.PApplet;

import java.lang.reflect.InvocationTargetException;
import java.util.List;


public interface PAppletFactory<T extends PApplet>
{
  T createInstance( ProcessingSketchApplication<T> context, List<String> args )
    throws InvocationTargetException;


  default T createInstance( ProcessingSketchApplication<T> context, String... args )
    throws InvocationTargetException
  {
    return createInstance(context, Arrays.asImmutableList(args));
  }
}
