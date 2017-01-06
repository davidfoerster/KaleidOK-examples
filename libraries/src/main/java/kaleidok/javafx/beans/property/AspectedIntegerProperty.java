package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.Map;


public class AspectedIntegerProperty extends ReadOnlyIntegerWrapper
  implements AspectedProperty<Number>
{
  public AspectedIntegerProperty() { }

  public AspectedIntegerProperty( int initialValue )
  {
    super(initialValue);
  }

  public AspectedIntegerProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedIntegerProperty( Object bean, String name, int initialValue )
  {
    super(bean, name, initialValue);
  }


  private final Map<PropertyAspectTag<?, ? super Number>, Object> aspectMap =
    new HashMap<>();


  @SuppressWarnings("unchecked")
  @Override
  public <A> A getAspect( PropertyAspectTag<A, ? super Number> tag )
  {
    return (A) aspectMap.get(tag);
  }


  @Override
  public <A> A addAspect( PropertyAspectTag<A, ? super Number> tag, A aspect )
  {
    return AspectHelper.addAspect(this, aspectMap, tag, aspect);
  }
}
