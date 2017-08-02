package kaleidok.javafx.util.converter;

import javafx.util.StringConverter;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.text.*;
import java.util.Objects;


public class CachingFormattedStringConverter<T, F extends Format>
  extends StringConverter<T> implements Cloneable
{
  private F format;

  private String toStringCachedString;

  private T toStringCachedValue;

  private String fromStringCachedString;

  private T fromStringCachedValue;

  private StringBuffer cachedStringBuffer;

  private FieldPosition cachedFieldPosition;

  private ParsePosition cachedParsePosition;


  public CachingFormattedStringConverter( F format )
  {
    this.format = Objects.requireNonNull(format);
  }


  @Override
  public String toString( T value )
  {
    if (value == null)
      return null;

    if (value.equals(toStringCachedValue))
      return toStringCachedString;

    String result = toStringCachedString =
      getFormat()
        .format(value, getStringBufferInstance(), getFieldPositionInstance())
        .toString();
    toStringCachedValue = value;
    return result;
  }


  @Override
  public T fromString( String source )
  {
    if (source == null)
      return null;

    source = source.trim();
    if (source.isEmpty())
      return null;

    if (source.equals(fromStringCachedString))
      return fromStringCachedValue;

    ParsePosition parsePosition = getParsePositionInstance();
    Object result = getFormat().parseObject(source, parsePosition);
    if (parsePosition.getIndex() == source.length())
    {
      T typedResult = fromStringCachedValue = convertParseResult(result);
      fromStringCachedString = source;
      return typedResult;
    }

    Exception ex =
      Objects.requireNonNull(getParseException(source, parsePosition));
    throw (ex instanceof RuntimeException) ?
      (RuntimeException) ex :
      new IllegalArgumentException(ex);
  }


  @SuppressWarnings("unchecked")
  protected T convertParseResult( Object parseResult )
  {
    return (T) parseResult;
  }


  protected Exception getParseException(
    @SuppressWarnings("unused") String source, ParsePosition pos )
  {
    return new ParseException("Couldn’t parse source string",
      pos.getErrorIndex());
  }


  public F getFormat()
  {
    return format;
  }


  public void setFormat( F format )
  {
    if (format != this.format)
    {
      this.format = Objects.requireNonNull(format);
      resetCaches();
    }
  }


  @OverridingMethodsMustInvokeSuper
  public void resetCaches()
  {
    fromStringCachedValue = null;
    fromStringCachedString = null;
    toStringCachedValue = null;
    toStringCachedString = null;
  }


  protected StringBuffer getStringBufferInstance()
  {
    StringBuffer cachedStringBuffer = this.cachedStringBuffer;
    if (cachedStringBuffer == null)
    {
      this.cachedStringBuffer = cachedStringBuffer = new StringBuffer();
    }
    else
    {
      cachedStringBuffer.setLength(0);
    }
    return cachedStringBuffer;
  }


  protected FieldPosition getFieldPositionInstance()
  {
    FieldPosition cachedFieldPosition = this.cachedFieldPosition;
    if (cachedFieldPosition == null)
    {
      this.cachedFieldPosition = cachedFieldPosition = new FieldPosition(0);
    }
    else
    {
      cachedFieldPosition.setBeginIndex(0);
      cachedFieldPosition.setEndIndex(0);
    }
    return cachedFieldPosition;
  }


  protected ParsePosition getParsePositionInstance()
  {
    ParsePosition cachedParsePosition = this.cachedParsePosition;
    if (cachedParsePosition == null)
    {
      this.cachedParsePosition = cachedParsePosition = new ParsePosition(0);
    }
    else
    {
      cachedParsePosition.setIndex(0);
      cachedParsePosition.setErrorIndex(-1);
    }
    return cachedParsePosition;
  }


  @Override
  @SuppressWarnings("unchecked")
  public CachingFormattedStringConverter<T, F> clone()
  {
    CachingFormattedStringConverter<T, F> clone;
    try {
      clone = (CachingFormattedStringConverter<T, F>) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new InternalError(ex);
    }
    clone.format = (F) clone.format.clone();
    clone.cachedStringBuffer = null;
    clone.cachedFieldPosition = null;
    clone.cachedParsePosition = null;
    return clone;
  }
}
