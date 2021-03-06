package kaleidok.image.filter;

import kaleidok.image.filter.HSBAdjustFilter.FilterMode;
import kaleidok.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.image.RGBImageFilter;
import java.text.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;


public class RGBImageFilterFormat extends Format
{
  private static final long serialVersionUID = 5703498461771829445L;

  public static final char SPACE = ' ';


  private NumberFormat numberFormat;

  public final char argumentDelimiter, argumentEnclosingStart, argumentEnclosingEnd;


  public RGBImageFilterFormat( NumberFormat numberFormat,
    char argumentDelimiter, char argumentEnclosingStart,
    char argumentEnclosingEnd )
  {
    if (argumentDelimiter == argumentEnclosingStart ||
      argumentDelimiter == argumentEnclosingEnd)
    {
      throw new IllegalArgumentException(
        "The argument delimiter mustn’t be equal to either of the enclosing " +
          "characters");
    }

    char[] delimiters = {
        argumentDelimiter, argumentEnclosingStart, argumentEnclosingEnd
      };
    if (ArrayUtils.contains(delimiters, SPACE) ||
      Arrays.stream(delimiters).anyMatch(Character::isDigit))
    {
      throw new IllegalArgumentException(
        "The delimiters and enclosures mustn’t be spaces or control " +
          "characters or digits");
    }

    if (Objects.requireNonNull(numberFormat) instanceof DecimalFormat)
    {
      DecimalFormat dFmt = (DecimalFormat) numberFormat;
      DecimalFormatSymbols dFmtSym = dFmt.getDecimalFormatSymbols();
      if (ArrayUtils.contains(delimiters, dFmtSym.getDecimalSeparator()) ||
        (dFmt.isGroupingUsed() &&
           ArrayUtils.contains(delimiters, dFmtSym.getGroupingSeparator())))
      {
        throw new IllegalArgumentException(
          "The delimiters and enclosures mustn’t be the same as the decimal " +
            "or grouping separators");
      }
      if (StringUtils.containsAny(dFmt.getPositivePrefix(), delimiters) ||
        StringUtils.containsAny(dFmt.getNegativePrefix(), delimiters) ||
        ArrayUtils.contains(delimiters, dFmtSym.getMinusSign()) ||
        StringUtils.containsAny(dFmtSym.getExponentSeparator(), delimiters))
      {
        throw new IllegalArgumentException(
          "The argument delimiter mustn’t appear in the prefixes for positive " +
            "or negative numbers or the exponent separator");
      }
    }

    this.numberFormat = numberFormat;
    this.argumentDelimiter = argumentDelimiter;
    this.argumentEnclosingStart = argumentEnclosingStart;
    this.argumentEnclosingEnd = argumentEnclosingEnd;
  }


  public RGBImageFilterFormat( NumberFormat numberFormat )
  {
    this(numberFormat, ',', '(', ')');
  }


  public RGBImageFilterFormat()
  {
    this(getDefaultNumberFormat());
  }


  private static NumberFormat getDefaultNumberFormat()
  {
    NumberFormat fmt = NumberFormat.getInstance(Locale.ROOT);
    fmt.setGroupingUsed(false);
    return fmt;
  }


  @Override
  public StringBuffer format( Object obj, StringBuffer toAppendTo,
    FieldPosition pos )
  {
    if (!(obj instanceof HSBAdjustFilter))
    {
      throw new IllegalArgumentException(
        "Must be an instance of " + HSBAdjustFilter.class.getName());
    }

    return format((HSBAdjustFilter) obj, toAppendTo, pos);
  }


  public StringBuffer format( HSBAdjustFilter filter, StringBuffer sb,
    FieldPosition pos )
  {
    Object filterMode = filter.filterMode;
    if (!(filterMode instanceof FilterMode))
    {
      throw new IllegalArgumentException(
        "Filter mode is no instance of " + FilterMode.class.getName());
    }

    sb.ensureCapacity(sb.length() + 30);
    sb.append(((FilterMode) filterMode).symbol)
      .append("hsb").append(argumentEnclosingStart);
    numberFormat.format(filter.hue, sb, pos)
      .append(argumentDelimiter).append(SPACE);
    numberFormat.format(filter.saturation, sb, pos)
      .append(argumentDelimiter).append(SPACE);
    numberFormat.format(filter.brightness, sb, pos)
      .append(argumentEnclosingEnd);

    return sb;
  }


  @Override
  public RGBImageFilter parseObject( String s, ParsePosition pos )
  {
    int len = s.length();
    if (pos.getIndex() >= len)
      return parseError(pos);

    // parse filter mode
    final int initialIndex = pos.getIndex();
    FilterMode filterMode;
    if (Character.isJavaIdentifierStart(s.charAt(pos.getIndex())))
    {
      filterMode = FilterMode.ADD;
    }
    else try
    {
      filterMode = FilterMode.fromSymbol(s.charAt(pos.getIndex()));
      pos.setIndex(pos.getIndex() + 1);
    }
    catch (IllegalArgumentException ignored)
    {
      return parseError(pos);
    }

    // parse color model name
    int i = pos.getIndex();
    if (i >= len || !Character.isJavaIdentifierStart(s.charAt(i)))
      return parseError(pos);
    int regionEnd = s.indexOf(argumentEnclosingStart, ++i);
    if (regionEnd < 0)
      return parseError(pos);
    for (; i < regionEnd; i++)
      if (!Character.isJavaIdentifierPart(s.charAt(i)))
        return parseError(pos, i);
    String colorModel = s.substring(pos.getIndex(), regionEnd);

    // parse argument region
    i = regionEnd + 1;
    while (i < len && s.charAt(i) <= SPACE)
      i++;
    pos.setIndex(i);
    regionEnd = s.indexOf(argumentEnclosingEnd, i);
    if (regionEnd < 0)
      return parseError(pos);
    i = regionEnd - 1;
    while (i > pos.getIndex() && s.charAt(i) <= SPACE)
      i--;
    List<Number> args = parseNumericArguments(s, pos, ++i, null);
    if (args == null)
      return null;
    pos.setIndex(regionEnd + 1);

    // create filter
    if ("hsb".equalsIgnoreCase(colorModel))
    {
      if (args.size() == 3)
      {
        return new HSBAdjustFilter(
          args.get(0).floatValue(), args.get(1).floatValue(),
          args.get(2).floatValue(), filterMode);
      }
    }

    return parseError(pos, initialIndex);
  }


  private List<Number> parseNumericArguments( String s, ParsePosition pos,
    int regionEnd, Predicate<Number> numberPredicate )
  {
    List<Number> args = null;
    int i = pos.getIndex();
    Number arg;
    while ((arg = numberFormat.parse(s, pos)) != null)
    {
      if (numberPredicate != null && !numberPredicate.test(arg))
        return parseError(pos, i);

      if (args == null)
        args = new ArrayList<>();
      args.add(arg);

      i = pos.getIndex();
      while (i < regionEnd && s.charAt(i) <= SPACE)
        i++;
      if (i == regionEnd)
      {
        pos.setIndex(i);
        return args;
      }
      if (s.charAt(i) == argumentDelimiter)
      {
        do {
          i++;
        } while (i < regionEnd && s.charAt(i) <= SPACE);
      }
      else if (s.charAt(pos.getIndex() - 1) != argumentDelimiter)
      {
        return parseError(pos, i);
      }
      pos.setIndex(i);
    }

    return parseError(pos);
  }


  private static <T> T parseError( ParsePosition pos )
  {
    return parseError(pos, pos.getIndex());
  }

  @SuppressWarnings("SameReturnValue")
  private static <T> T parseError( ParsePosition pos, int errorIndex )
  {
    pos.setErrorIndex(errorIndex);
    return null;
  }


  @Override
  public RGBImageFilterFormat clone()
  {
    RGBImageFilterFormat clone = (RGBImageFilterFormat) super.clone();
    clone.numberFormat = (NumberFormat) clone.numberFormat.clone();
    return clone;
  }
}
