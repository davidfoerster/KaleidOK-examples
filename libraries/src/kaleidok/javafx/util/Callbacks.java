package kaleidok.javafx.util;

import javafx.util.Callback;

import java.util.Objects;
import java.util.function.Supplier;


public final class Callbacks
{
  private Callbacks() { }


  public static <T, R> Callback<R, T> ignoreArg( final Supplier<T> f )
  {
    Objects.requireNonNull(f);
    return (x) -> f.get();
  }
}
