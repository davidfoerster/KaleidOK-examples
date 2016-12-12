package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.ObservableList;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AspectedListProperty<E> extends ReadOnlyListWrapper<E>
  implements AspectedProperty<ObservableList<E>>
{
  public AspectedListProperty() { }

  public AspectedListProperty( ObservableList<E> initialValue )
  {
    super(initialValue);
  }

  public AspectedListProperty( Object bean, String name )
  {
    super(bean, name);
  }

  public AspectedListProperty( Object bean, String name,
    ObservableList<E> initialValue )
  {
    super(bean, name, initialValue);
  }


  private final Map<PropertyAspectTag<?, ? super ObservableList<E>>, Object> aspectMap =
    new HashMap<>();


  @SuppressWarnings("unchecked")
  @Override
  public <A> A getAspect( PropertyAspectTag<A, ? super ObservableList<E>> tag )
  {
    return (A) aspectMap.get(tag);
  }


  @Override
  public <A> A addAspect( PropertyAspectTag<A, ? super ObservableList<E>> tag,
    A aspect )
  {
    return AspectHelper.addAspect(this, aspectMap, tag, aspect);
  }
}
