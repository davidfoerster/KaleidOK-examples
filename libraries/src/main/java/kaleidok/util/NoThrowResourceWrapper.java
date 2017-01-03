package kaleidok.util;

import java.util.Objects;
import java.util.function.Consumer;


public class NoThrowResourceWrapper<T> extends AbstractResourceWrapper<T>
{
  protected final Consumer<? super T> closer;


  public NoThrowResourceWrapper( T resource, Consumer<? super T> closer )
  {
    super(resource);
    this.closer = Objects.requireNonNull(closer);
  }


  @Override
  public void close()
  {
    T resource = get();
    if (resource != null)
      closer.accept(resource);
  }
}
