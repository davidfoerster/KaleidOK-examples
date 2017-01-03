package kaleidok.javafx.beans.property;

import javafx.beans.property.ReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.InstantiatingPropertyAspectTag;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;


public interface AspectedReadOnlyProperty<T>
  extends ReadOnlyProperty<T>
{
  <A> A getAspect( PropertyAspectTag<A, ? super T> tag );

  <A> A addAspect( PropertyAspectTag<A, ? super T> tag, A aspect );

  default <A> A addAspect( InstantiatingPropertyAspectTag<A, ? super T> tag )
  {
    return addAspect(tag, null);
  }
}
