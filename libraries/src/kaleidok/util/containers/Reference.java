package kaleidok.util.containers;

public class Reference<T>
{
  public T item;

  public Reference( T item )
  {
    this.item = item;
  }

  public Reference()
  {
    this(null);
  }
}
