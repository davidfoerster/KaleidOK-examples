package kaleidok.javafx.beans.property;

import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;

import java.util.Map;
import java.util.Objects;


final class AspectHelper
{
  private AspectHelper() { }


  static <A, T> A addAspect( final AspectedReadOnlyProperty<T> property,
    Map<PropertyAspectTag<?, ? super T>, Object> aspectMap,
    PropertyAspectTag<A, ? super T> tag, final A aspect )
  {
    @SuppressWarnings("unchecked")
    A newAspect = (A) aspectMap.compute(tag,
      (tag1, currentAspect) ->
      {
        if (currentAspect != null)
        {
          throw new IllegalStateException(String.format(
            "Property %s already has an aspect with tag %s: %s",
            property, tag1, currentAspect));
        }
        return Objects.requireNonNull(
          ((PropertyAspectTag<A, ? super T>) tag1).setup(aspect, property),
          "aspect");
      });

    assert newAspect != null;

    return newAspect;
  }
}
