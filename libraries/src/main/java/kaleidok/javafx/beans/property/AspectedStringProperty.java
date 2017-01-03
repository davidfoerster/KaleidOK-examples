package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyStringWrapper;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.Map;


public class AspectedStringProperty extends ReadOnlyStringWrapper
  implements AspectedProperty<String>
{
  public AspectedStringProperty() { }

  public AspectedStringProperty( String initialValue )
  {
    super(initialValue);
  }

  public AspectedStringProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedStringProperty( Object bean, String name, String initialValue )
  {
    super(bean, name, initialValue);
  }


  private final Map<PropertyAspectTag<?, ? super String>, Object> aspectMap =
    new HashMap<>();


  @SuppressWarnings("unchecked")
  @Override
  public <A> A getAspect( PropertyAspectTag<A, ? super String> tag )
  {
    return (A) aspectMap.get(tag);
  }


  @Override
  public <A> A addAspect( PropertyAspectTag<A, ? super String> tag, A aspect )
  {
    return AspectHelper.addAspect(this, aspectMap, tag, aspect);
  }
}
