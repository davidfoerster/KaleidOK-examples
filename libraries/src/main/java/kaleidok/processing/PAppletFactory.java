package kaleidok.processing;

import kaleidok.util.Arrays;
import processing.core.PApplet;

import java.util.List;
import java.util.function.Consumer;


public interface PAppletFactory<T extends PApplet>
{
  T createInstance( ProcessingSketchApplication<T> context,
    Consumer<? super T> callback, List<String> args );


  default T createInstance( ProcessingSketchApplication<T> context,
    Consumer<? super T> callback, String... args )
  {
    return createInstance(context, callback, Arrays.asImmutableList(args));
  }
}
