package kaleidok.javafx.beans.property.binding;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import kaleidok.text.FormatUtils;
import kaleidok.text.IMessageFormat;
import org.apache.commons.lang3.StringUtils;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;


public class MessageFormatBinding
  extends ObjectBinding<MessageFormat> implements IMessageFormat
{
  protected final ObservableValue<String> formatString;

  public Object[] testArgs;

  public Predicate<? super CharSequence> resultVerifier = null;


  public MessageFormatBinding( ObservableValue<String> formatString )
  {
    bind(this.formatString = Objects.requireNonNull(formatString));
  }


  @Override
  protected MessageFormat computeValue()
  {
    String s = formatString.getValue();
    return (s != null && !s.isEmpty()) ?
      FormatUtils.verifyFormatThrowing(
        ( _s ) -> new MessageFormat(_s, Locale.ENGLISH),
        s, testArgs,
        (resultVerifier != null) ? resultVerifier : StringUtils::isNotEmpty,
        null) :
      null;
  }


  @Override
  public void dispose()
  {
    unbind(formatString);
  }


  @Override
  public boolean isAvailable()
  {
    return get() != null;
  }


  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  private static StringBuffer format( final MessageFormat fmt,
    Object[] formatArgs, StringBuffer buffer, FieldPosition pos )
    throws IllegalArgumentException
  {
    synchronized (fmt)
    {
      return fmt.format(formatArgs, buffer, pos);
    }
  }


  @Override
  public StringBuffer format( Object[] formatArgs, StringBuffer buffer,
    FieldPosition pos )
    throws IllegalArgumentException
  {
    MessageFormat fmt = get();
    return (fmt != null) ? format(fmt, formatArgs, buffer, pos) : null;
  }


  @Override
  public String format( Object... formatArgs )
    throws IllegalArgumentException
  {
    MessageFormat fmt = get();
    return (fmt != null) ?
      format(fmt, formatArgs, new StringBuffer(), null).toString() :
      null;
  }


  @Override
  public MessageFormat copy()
  {
    MessageFormat fmt = get();
    return (fmt != null) ? (MessageFormat) fmt.clone() : null;
  }


  @Override
  public String toPattern()
  {
    MessageFormat fmt = get();
    return (fmt != null) ? fmt.toPattern() : null;
  }


  private IMessageFormat readOnlyFormat = null;

  public IMessageFormat asReadOnlyFormat()
  {
    if (readOnlyFormat == null)
      readOnlyFormat = new ReadOnlyMessageFormat();
    return readOnlyFormat;
  }


  private class ReadOnlyMessageFormat implements IMessageFormat
  {
    @Override
    public boolean isAvailable()
    {
      return MessageFormatBinding.this.isAvailable();
    }


    @Override
    public StringBuffer format( Object[] formatArgs, StringBuffer buffer,
      FieldPosition pos )
      throws IllegalArgumentException
    {
      return MessageFormatBinding.this.format(formatArgs, buffer, pos);
    }


    @Override
    public String format( Object... formatArgs )
      throws IllegalArgumentException
    {
      return MessageFormatBinding.this.format(formatArgs);
    }


    @Override
    public MessageFormat copy()
    {
      return MessageFormatBinding.this.copy();
    }


    @Override
    public String toPattern()
    {
      return MessageFormatBinding.this.toPattern();
    }
  }
}
