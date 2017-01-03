package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.Map;


public class AspectedBooleanProperty extends ReadOnlyBooleanWrapper
  implements AspectedProperty<Boolean>
{
  public AspectedBooleanProperty() { }

  public AspectedBooleanProperty( boolean initialValue )
  {
    super(initialValue);
  }

  public AspectedBooleanProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedBooleanProperty( Object bean, String name, boolean initialValue )
  {
    super(bean, name, initialValue);
  }


  private final Map<PropertyAspectTag<?, ? super Boolean>, Object> aspectMap =
    new HashMap<>();


  @SuppressWarnings("unchecked")
  @Override
  public <A> A getAspect( PropertyAspectTag<A, ? super Boolean> tag )
  {
    return (A) aspectMap.get(tag);
  }


  @Override
  public <A> A addAspect( PropertyAspectTag<A, ? super Boolean> tag, A aspect )
  {
    return AspectHelper.addAspect(this, aspectMap, tag, aspect);
  }
}
