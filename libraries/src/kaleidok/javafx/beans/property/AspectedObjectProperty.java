package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyObjectWrapper;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.Map;


public class AspectedObjectProperty<T> extends ReadOnlyObjectWrapper<T>
  implements AspectedProperty<T>
{
  public AspectedObjectProperty() { }

  public AspectedObjectProperty( T initialValue )
  {
    super(initialValue);
  }

  public AspectedObjectProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedObjectProperty( Object bean, String name, T initialValue )
  {
    super(bean, name, initialValue);
  }


  private final Map<PropertyAspectTag<?, ? super T>, Object> aspectMap =
    new HashMap<>();


  @SuppressWarnings("unchecked")
  @Override
  public <A> A getAspect( PropertyAspectTag<A, ? super T> tag )
  {
    return (A) aspectMap.get(tag);
  }


  @Override
  public <A> A addAspect( PropertyAspectTag<A, ? super T> tag, A aspect )
  {
    return AspectHelper.addAspect(this, aspectMap, tag, aspect);
  }
}
