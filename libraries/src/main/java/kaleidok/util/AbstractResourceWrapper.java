package kaleidok.util;


import java.util.Objects;
import java.util.concurrent.Callable;
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


  public static <T extends Throwable> T closeExceptional(
    AutoCloseable resource, T thrown )
  {
    try
    {
      resource.close();
    }
    catch (Exception ex)
    {
      thrown.addSuppressed(ex);
    }
    return thrown;
  }


  public static <T extends Throwable> T closeExceptional(
    Callable<Void> closer, T thrown )
  {
    try
    {
      closer.call();
    }
    catch (Exception ex)
    {
      thrown.addSuppressed(ex);
    }
    return thrown;
  }
}
