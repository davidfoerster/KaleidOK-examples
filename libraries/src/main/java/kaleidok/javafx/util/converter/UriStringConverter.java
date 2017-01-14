package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;

import java.net.URI;


public class UriStringConverter extends StringConverter<URI>
{
  public static final UriStringConverter INSTANCE = new UriStringConverter();


  protected UriStringConverter() { }


  @Override
  public String toString( URI object )
  {
    return (object != null) ? object.toString() : null;
  }


  @Override
  public URI fromString( String string )
  {
    if (string == null || string.isEmpty())
      return null;

    URI value = URI.create(string);

    if (!value.isAbsolute() || value.isOpaque())
      throw new IllegalArgumentException("relative or opaque URI");

    return value;
  }
}
