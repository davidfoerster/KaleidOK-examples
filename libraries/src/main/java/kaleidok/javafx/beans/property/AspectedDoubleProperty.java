package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyDoubleWrapper;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.Map;


public class AspectedDoubleProperty extends ReadOnlyDoubleWrapper
  implements AspectedProperty<Number>
{
  public AspectedDoubleProperty() { }

  public AspectedDoubleProperty( double initialValue )
  {
    super(initialValue);
  }

  public AspectedDoubleProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedDoubleProperty( Object bean, String name, double initialValue )
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
