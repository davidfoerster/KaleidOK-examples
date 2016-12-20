package kaleidok.test.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.math.BigDecimal;


public class BigDecimalComparesEqual extends TypeSafeDiagnosingMatcher<Number>
{
  private final BigDecimal expectedValue;


  public BigDecimalComparesEqual( BigDecimal expectedValue )
  {
    super(BigDecimal.class);
    this.expectedValue = expectedValue;
  }


  public static BigDecimalComparesEqual comparesEqual( BigDecimal expectedValue )
  {
    return new BigDecimalComparesEqual(expectedValue);
  }


  @Override
  public void describeTo( Description description )
  {
    description.appendValue(expectedValue);
  }


  @Override
  protected boolean matchesSafely( Number o, Description mismatchDescription )
  {
    boolean result = expectedValue.compareTo((BigDecimal) o) == 0;
    if (!result)
      mismatchDescription.appendValue(o);
    return result;
  }
}
