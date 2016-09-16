package kaleidok.util.function;

import java.util.function.Supplier;


public class InstanceSupplier<T> implements Supplier<T>
{
  private final T o;


  public InstanceSupplier( T o )
  {
    this.o = o;
  }


  @Override
  public T get()
  {
    return o;
  }
}
