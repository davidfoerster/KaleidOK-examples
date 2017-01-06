package kaleidok.util;


public class ResourceWrapper<T extends AutoCloseable>
  extends AbstractResourceWrapper<T>
{
  protected ResourceWrapper( T resource )
  {
    super(resource);
  }


  @SuppressWarnings("resource")
  @Override
  public void close() throws Exception
  {
    T resource = get();
    if (resource != null)
      resource.close();
  }
}
