package kaleidok.text;

import java.math.RoundingMode;
import java.text.*;
import java.util.Objects;


public abstract class DecimalFormatDelegator extends Format
{
  private static final long serialVersionUID = 773102108080567437L;

  private DecimalFormat underlying;


  protected DecimalFormatDelegator( DecimalFormat underlying )
  {
    this.underlying = Objects.requireNonNull(underlying);
  }


  protected DecimalFormatDelegator()
  {
    this.underlying = new DecimalFormat();
  }


  public DecimalFormat getUnderlying()
  {
    return underlying;
  }

  public void setUnderlying( DecimalFormat underlying )
  {
    this.underlying = Objects.requireNonNull(underlying);
  }


  public StringBuffer format( double number, StringBuffer result, FieldPosition fieldPosition )
  {
    return underlying.format(number, result, fieldPosition);
  }


  public StringBuffer format( long number, StringBuffer result, FieldPosition fieldPosition )
  {
    return underlying.format(number, result, fieldPosition);
  }


  public Number parse( String text, ParsePosition pos )
  {
    return underlying.parse(text, pos);
  }


  public DecimalFormatSymbols getDecimalFormatSymbols()
  {
    return underlying.getDecimalFormatSymbols();
  }

  public void setDecimalFormatSymbols( DecimalFormatSymbols newSymbols )
  {
    underlying.setDecimalFormatSymbols(newSymbols);
  }

  public String getPositivePrefix()
  {
    return underlying.getPositivePrefix();
  }

  public void setPositivePrefix( String newValue )
  {
    underlying.setPositivePrefix(newValue);
  }

  public String getNegativePrefix()
  {
    return underlying.getNegativePrefix();
  }

  public void setNegativePrefix( String newValue )
  {
    underlying.setNegativePrefix(newValue);
  }

  public String getPositiveSuffix()
  {
    return underlying.getPositiveSuffix();
  }

  public void setPositiveSuffix( String newValue )
  {
    underlying.setPositiveSuffix(newValue);
  }

  public String getNegativeSuffix()
  {
    return underlying.getNegativeSuffix();
  }

  public void setNegativeSuffix( String newValue )
  {
    underlying.setNegativeSuffix(newValue);
  }

  public int getMultiplier()
  {
    return underlying.getMultiplier();
  }

  public void setMultiplier( int newValue )
  {
    underlying.setMultiplier(newValue);
  }

  public void setGroupingUsed( boolean newValue )
  {
    underlying.setGroupingUsed(newValue);
  }

  public int getGroupingSize()
  {
    return underlying.getGroupingSize();
  }

  public void setGroupingSize( int newValue )
  {
    underlying.setGroupingSize(newValue);
  }

  public boolean isDecimalSeparatorAlwaysShown()
  {
    return underlying.isDecimalSeparatorAlwaysShown();
  }

  public void setDecimalSeparatorAlwaysShown( boolean newValue )
  {
    underlying.setDecimalSeparatorAlwaysShown(newValue);
  }

  public boolean isParseBigDecimal()
  {
    return underlying.isParseBigDecimal();
  }

  public void setParseBigDecimal( boolean newValue )
  {
    underlying.setParseBigDecimal(newValue);
  }

  public void setMaximumIntegerDigits( int newValue )
  {
    underlying.setMaximumIntegerDigits(newValue);
  }

  public void setMinimumIntegerDigits( int newValue )
  {
    underlying.setMinimumIntegerDigits(newValue);
  }

  public void setMaximumFractionDigits( int newValue )
  {
    underlying.setMaximumFractionDigits(newValue);
  }

  public void setMinimumFractionDigits( int newValue )
  {
    underlying.setMinimumFractionDigits(newValue);
  }

  public int getMaximumIntegerDigits()
  {
    return underlying.getMaximumIntegerDigits();
  }

  public int getMinimumIntegerDigits()
  {
    return underlying.getMinimumIntegerDigits();
  }

  public int getMaximumFractionDigits()
  {
    return underlying.getMaximumFractionDigits();
  }

  public int getMinimumFractionDigits()
  {
    return underlying.getMinimumFractionDigits();
  }

  public RoundingMode getRoundingMode()
  {
    return underlying.getRoundingMode();
  }

  public void setRoundingMode( RoundingMode roundingMode )
  {
    underlying.setRoundingMode(roundingMode);
  }


  public String format( double number )
  {
    return format(number, new StringBuffer(), new FieldPosition(0)).toString();
  }


  public String format( long number )
  {
    return format(number, new StringBuffer(), new FieldPosition(0)).toString();
  }


  @Override
  public Number parseObject( String source, ParsePosition pos )
  {
    return parse(source, pos);
  }


  public Number parse( String source ) throws ParseException
  {
    ParsePosition parsePosition = new ParsePosition(0);
    Number result = parse(source, parsePosition);
    if (parsePosition.getIndex() > 0 && parsePosition.getErrorIndex() < 0)
      return result;

    throw new ParseException("Unparseable number: " + source,
      parsePosition.getErrorIndex());
  }


  public boolean isParseIntegerOnly()
  {
    return underlying.isParseIntegerOnly();
  }

  public void setParseIntegerOnly( boolean value )
  {
    underlying.setParseIntegerOnly(value);
  }

  public boolean isGroupingUsed()
  {
    return underlying.isGroupingUsed();
  }


  @Override
  public DecimalFormatDelegator clone()
  {
    DecimalFormatDelegator clone = (DecimalFormatDelegator) super.clone();
    if (clone.underlying != null)
      clone.underlying = (DecimalFormat) clone.underlying.clone();
    return clone;
  }
}
