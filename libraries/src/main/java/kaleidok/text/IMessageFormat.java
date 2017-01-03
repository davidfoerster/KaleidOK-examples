package kaleidok.text;

import java.text.FieldPosition;
import java.text.MessageFormat;


public interface IMessageFormat
{
  boolean isAvailable();


  StringBuffer format( Object[] formatArgs, StringBuffer buffer,
    FieldPosition pos ) throws IllegalArgumentException;


  String format( Object... formatArgs ) throws IllegalArgumentException;


  MessageFormat copy();


  String toPattern();
}
