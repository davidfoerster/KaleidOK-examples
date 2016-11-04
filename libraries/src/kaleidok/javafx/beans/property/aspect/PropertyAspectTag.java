package kaleidok.javafx.beans.property.aspect;

import javafx.beans.property.ReadOnlyProperty;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.util.Reflection;

import java.lang.reflect.Type;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaleidok.util.AssertionUtils.fastAssert;


public class PropertyAspectTag<A, T>
{
  protected PropertyAspectTag() { }


  public A setup( A aspect,
    @SuppressWarnings("unused") AspectedReadOnlyProperty<? extends T> property )
  {
    return aspect;
  }


  private String stringRepresentation = null;

  @Override
  public String toString()
  {
    if (stringRepresentation == null)
      stringRepresentation = makeStringRepresentation();
    return stringRepresentation;
  }


  protected String makeStringRepresentation()
  {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    final Class<PropertyAspectTag> baseClass =
      (Class<PropertyAspectTag>) getTagClass();
    final Type[] typeArguments =
      Reflection.getTypeArguments(getClass(), baseClass);
    @SuppressWarnings("NonConstantStringShouldBeStringBuffer")
    String s = baseClass.getSimpleName();
    fastAssert(!s.isEmpty());

    if (typeArguments != null && typeArguments.length != 0)
    {
      Stream<String> typeArgumentNames = Stream.of(typeArguments)
        .map((t) -> {
            String n = ((Class<?>) t).getSimpleName();
            fastAssert(!n.isEmpty());
            return n;
          });

      s += '<' + typeArgumentNames.collect(Collectors.joining(", ")) + '>';
    }

    return s;
  }


  @SuppressWarnings("rawtypes")
  protected Class<? extends PropertyAspectTag> getTagClass()
  {
    return getClass();
  }


  public A of( AspectedReadOnlyProperty<T> property )
  {
    return property.getAspect(this);
  }


  public A of( ReadOnlyProperty<T> property )
  {
    return (property instanceof AspectedReadOnlyProperty) ?
      of((AspectedReadOnlyProperty<T>) property) :
      null;
  }


  @SuppressWarnings("unchecked")
  public A ofAny( AspectedReadOnlyProperty<?> property )
  {
    return property.getAspect((PropertyAspectTag<A, Object>) this);
  }


  public A ofAny( ReadOnlyProperty<?> property )
  {
    return (property instanceof AspectedReadOnlyProperty) ?
      ofAny((AspectedReadOnlyProperty<?>) property) :
      null;
  }
}
