package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CollectionStringConverter<E, C extends Collection<E>>
  extends StringConverter<C>
{
  public final Function<? super E, String> elementToString;

  public final Function<? super String, ? extends E> elementFromString;

  public final Collector<E, ?, ? extends C> collector;

  public final Collector<CharSequence, ?, String> stringJoiner;

  public final Pattern stringSplitter;

  public Function<? super List<CharSequence>, ? extends Exception> paragraphVerifier = null;


  public CollectionStringConverter(
    final Function<? super E, String> elementToString,
    Function<? super String, ? extends E> elementFromString,
    final String delimiter, Collector<E, ?, ? extends C> collector )
  {
    Objects.requireNonNull(delimiter, "delimiter");
    if (delimiter.isEmpty())
      throw new IllegalArgumentException("Empty delimiter");

    Objects.requireNonNull(elementToString, "elementToString");
    this.elementToString = (obj) -> {
        String s = elementToString.apply(obj);
        if (s == null)
          throw new NullPointerException("Can't join null string");
        if (s.contains(delimiter))
          throw new IllegalArgumentException("List element contains delimiter");
        return s;
      };
    this.elementFromString =
      Objects.requireNonNull(elementFromString, "elementFromString");
    this.collector =
      Objects.requireNonNull(collector, "collector");
    this.stringJoiner = Collectors.joining(delimiter);
    this.stringSplitter = Pattern.compile(delimiter, Pattern.LITERAL);
  }


  public CollectionStringConverter( StringConverter<E> stringConverter,
    String delimiter, Collector<E, ?, ? extends C> collector )
  {
    this(stringConverter::toString, stringConverter::fromString, delimiter, collector);
  }


  public static <L extends Collection<String>> CollectionStringConverter<String, L>
  getInstance( String delimiter, Collector<String, ?, ? extends L> collector )
  {
    Function<String, String> identity = Function.identity();
    return new CollectionStringConverter<>(identity, identity, delimiter, collector);
  }


  @Override
  public String toString( C collection )
  {
    //noinspection unchecked
    return
      (collection == null) ?
        null :
      collection.isEmpty() ?
        "" :
      (collection.size() != 1) ?
        collection.stream().map(elementToString).collect(stringJoiner) :
        elementToString.apply(
          (collection instanceof List) ?
            ((List<E>) collection).get(0) :
            collection.iterator().next());
  }


  @Override
  public C fromString( String string )
  {
    return (string != null) ?
      (string.isEmpty() ?
          Stream.<E>empty() :
          stringSplitter.splitAsStream(string).map(elementFromString))
        .collect(collector) :
      null;
  }
}
