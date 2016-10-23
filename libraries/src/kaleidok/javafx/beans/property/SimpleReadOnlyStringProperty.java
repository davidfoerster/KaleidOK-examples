package kaleidok.javafx.beans.property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;


public class SimpleReadOnlyStringProperty extends ReadOnlyStringProperty
{
  private final Object bean;

  private final String name;

  private final String value;


  public SimpleReadOnlyStringProperty( Object bean, String name, String value )
  {
    this.bean = bean;
    this.name = (name != null) ? name : "";
    this.value = value;
  }


  public SimpleReadOnlyStringProperty( String value )
  {
    this(null, null, value);
  }


  @Override
  public Object getBean()
  {
    return bean;
  }


  @Override
  public String getName()
  {
    return name;
  }


  @Override
  public String get()
  {
    return value;
  }


  @Override
  public void addListener( ChangeListener<? super String> listener ) { }

  @Override
  public void removeListener( ChangeListener<? super String> listener ) { }

  @Override
  public void addListener( InvalidationListener listener ) { }

  @Override
  public void removeListener( InvalidationListener listener ) { }
}
