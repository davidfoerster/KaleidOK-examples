package kaleidok.util.function;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;


public final class Functions
{
  private Functions() { }


  public static <T> Function<Object, T> ignoreArg( final Supplier<T> f )
  {
    Objects.requireNonNull(f);
    return (x) -> f.get();
  }
}
