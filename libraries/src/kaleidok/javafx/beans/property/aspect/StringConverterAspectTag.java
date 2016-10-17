package kaleidok.javafx.beans.property.aspect;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedProperty;


public class StringConverterAspectTag<T>
  extends PropertyAspectTag<StringConverter<T>, T>
{
  private static final StringConverterAspectTag<?> INSTANCE =
    new StringConverterAspectTag<>();


  @SuppressWarnings("unchecked")
  public static <T> StringConverterAspectTag<T> getInstance()
  {
    return (StringConverterAspectTag<T>) INSTANCE;
  }


  protected StringConverterAspectTag() { }


  @SuppressWarnings("rawtypes")
  @Override
  protected final Class<? extends PropertyAspectTag> getTagClass()
  {
    return StringConverterAspectTag.class;
  }


  @Override
  public final int hashCode()
  {
    return 0x2843ecab;
  }


  @Override
  public final boolean equals( Object obj )
  {
    return obj instanceof StringConverterAspectTag;
  }


  public static <T> ReadOnlyStringWrapper bindBidirectional(
    AspectedProperty<T> property )
  {
    StringConverter<T> converter = property.getAspect(getInstance());
    if (converter == null)
      return null;

    ReadOnlyStringWrapper convertedProperty =
      new ReadOnlyStringWrapper(property.getBean(), property.getName());
    convertedProperty.bindBidirectional(property, converter);
    return convertedProperty;
  }
}
