package kaleidok.util;


import java.util.Objects;
import java.util.function.Function;


public abstract class AbstractResourceWrapper<T> implements AutoCloseable
{
  private T resource;


  protected AbstractResourceWrapper( T resource )
  {
    this.resource = resource;
  }


  public static <T> AbstractResourceWrapper<T> of( T resource,
    final Function<? super T, ? extends Exception> closer )
  {
    Objects.requireNonNull(closer);
    return new AbstractResourceWrapper<T>(resource)
      {
        @Override
        public void close() throws Exception
        {
          @SuppressWarnings("AnonymousClassVariableHidesContainingMethodVariable")
          T resource = get();
          if (resource != null)
          {
            Exception ex = closer.apply(resource);
            if (ex != null)
              throw ex;
          }
        }
      };
  }


  public T get()
  {
    return resource;
  }


  public void release()
  {
    this.resource = null;
  }
}
