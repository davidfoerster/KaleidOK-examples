package kaleidok.javafx.beans.binding;

import javafx.beans.property.Property;
import javafx.util.Callback;
import kaleidok.util.Math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;


public class BidirectionalNumberBinding<T extends Number>
  extends BidirectionalNumberBindingBase<T>
{

  private final Callback<? super Number, ? extends T> valueConverter;


  public BidirectionalNumberBinding( Property<T> p1, Property<Number> p2,
    Callback<? super Number, ? extends T> valueConverter )
  {
    super(p1, p2);
    this.valueConverter = Objects.requireNonNull(valueConverter, "value converter");
  }


  public BidirectionalNumberBinding( Property<T> p1, Property<Number> p2,
    Class<T> p1ValueType )
  {
    this(p1, p2, valueConverterFromClass(p1ValueType));
  }


  @Override
  protected T convertValue( Number v )
  {
    return valueConverter.call(v);
  }


  private static <T extends Number> Callback<? super Number, ? extends T>
  valueConverterFromClass( Class<T> valueType )
  {
    @SuppressWarnings("unchecked")
    Callback<? super Number, ? extends T> valueConverter =
      (Callback<? super Number, ? extends T>) valueConverters.get(valueType);
    if (valueConverter != null)
      return valueConverter;

    if (!Number.class.isAssignableFrom(valueType))
    {
      throw new ClassCastException(String.format(
        "Cannot convert %s to %s",
        valueType.getName(), Number.class.getName()));
    }
    throw new UnsupportedOperationException(String.format(
      "Conversion from %s to %s is unsupported",
      valueType.getName(), Number.class.getName()));
  }


  private static final Map<Class<? extends Number>, Callback<? super Number, ? extends Number>> valueConverters =
    new IdentityHashMap<Class<? extends Number>, Callback<? super Number, ? extends Number>>(16)
    {{
      put(Number.class, (v) -> v);
      put(Byte.class, (v) -> (v instanceof Byte) ? v : v.byteValue());
      put(Short.class, (v) -> (v instanceof Short) ? v : v.shortValue());
      put(Integer.class, (v) -> (v instanceof Integer) ? v : v.intValue());
      put(Long.class, (v) -> (v instanceof Long) ? v : v.longValue());
      put(Float.class, (v) -> (v instanceof Float) ? v : v.floatValue());
      put(Double.class, (v) -> (v instanceof Double) ? v : v.doubleValue());
      put(BigDecimal.class, Math::toBigDecimal);
      put(BigInteger.class, Math::toBigInteger);
    }};
}
