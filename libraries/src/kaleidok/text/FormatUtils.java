package kaleidok.text;

import org.apache.commons.lang3.tuple.Pair;

import java.text.FieldPosition;
import java.text.Format;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;


public final class FormatUtils
{
  private FormatUtils() { }


  public static boolean verifyFormat( Format fmt, Object fmtArg,
    Predicate<? super CharSequence> resultVerifier )
    throws IllegalArgumentException
  {
    CharSequence result =
      fmt.format(fmtArg, new StringBuffer(), new FieldPosition(0));
    return resultVerifier == null || resultVerifier.test(result);
  }


  public static <T, F extends Format> F verifyFormatThrowing(
    Function<? super T, F> fmtConstructor, T fmtConstructorArg, Object fmtArg,
    Predicate<? super CharSequence> resultVerifier,
    BiConsumer<? super IllegalArgumentException, ? super T> exceptionHandler )
    throws IllegalArgumentException
  {
    IllegalArgumentException caught;
    try
    {
      F fmt = fmtConstructor.apply(fmtConstructorArg);
      if (verifyFormat(fmt, fmtArg, resultVerifier))
        return fmt;
      caught = null;
    }
    catch (IllegalArgumentException ex)
    {
      caught = ex;
    }

    if (exceptionHandler != null)
    {
      exceptionHandler.accept(caught, fmtConstructorArg);
      return null;
    }

    String s =
      (fmtConstructorArg != null && fmtConstructorArg.getClass().isArray() &&
         !fmtConstructorArg.getClass().getComponentType().isPrimitive())
      ?
        Arrays.toString((Object[]) fmtConstructorArg) :
        String.valueOf(fmtConstructorArg);
    throw new IllegalArgumentException("Illegal format: " + s, caught);
  }


  public static void verifyFormatThrowing( final Format fmt, Object fmtArg,
    Predicate<? super CharSequence> resultVerifier, String errorMessageFormat )
    throws IllegalArgumentException
  {
    verifyFormatThrowing(
      (ignored) -> fmt,
      (errorMessageFormat != null) ? Pair.of(errorMessageFormat, fmt) : null,
      fmtArg, resultVerifier,
      (errorMessageFormat != null) ? FormatUtils::formattedExceptionHandler : null);
  }


  private static void formattedExceptionHandler( Throwable ex,
    Pair<String, Format> data )
  {
    throw new IllegalArgumentException(
      String.format(data.getLeft(), data.getRight()),
      ex);
  }
}
