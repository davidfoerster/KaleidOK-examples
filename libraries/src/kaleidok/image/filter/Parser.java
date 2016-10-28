package kaleidok.image.filter;

import java.awt.image.RGBImageFilter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;


public final class Parser
{
  public static final Pattern IMAGE_FILTER_SPEC_PATTERN = Pattern.compile(
    "(?<operator>[*/^+-]?)\\s*(?<colormodel>\\w+)\\s*\\(\\s*(?<args>[^\\s,)]+(?:\\s*,\\s*[^\\s,)]+)*)\\s*\\)",
    Pattern.CASE_INSENSITIVE);

  public static final Pattern ARGUMENT_DELIMITER_PATTERN = Pattern.compile("\\s*,\\s*");


  private Parser() { }


  public static RGBImageFilter parseFilter( String s )
  {
    if (s.isEmpty())
      return null;

    Matcher m = IMAGE_FILTER_SPEC_PATTERN.matcher(s);
    if (!m.matches())
      throw new IllegalArgumentException("Invalid filter spec: " + s);

    double[] args =
      ARGUMENT_DELIMITER_PATTERN.splitAsStream(m.group("args"))
        .mapToDouble(Double::parseDouble).toArray();
    if (!DoubleStream.of(args).allMatch(Double::isFinite))
    {
      throw new IllegalArgumentException(
        "Non-finite argument value encountered: " + m.group("args"));
    }

    int operatorStart = m.start("operator"),
      operatorLen = m.end("operator") - operatorStart;
    char operatorChar = (operatorLen != 0) ? s.charAt(operatorStart) : '+';
    HSBAdjustFilter.FilterMode filterMode;
    try
    {
      filterMode = HSBAdjustFilter.FilterMode.fromSymbol(operatorChar);
    }
    catch (IllegalArgumentException ex)
    {
      throw new AssertionError("Unexpected operator", ex);
    }

    //noinspection SpellCheckingInspection
    String colorModel = m.group("colormodel").toLowerCase(Locale.ROOT);
    switch (colorModel)
    {
    case "hsb":
      checkArgumentCount(3, args.length, colorModel);
      return new HSBAdjustFilter(
        (float) args[0], (float) args[1], (float) args[2], filterMode);

    case "rgb":
    default:
      throw new UnsupportedOperationException(
        "Unknown or unimplemented color model: " + colorModel);
    }
  }


  private static void checkArgumentCount( int expected, int actual,
    String methodName )
  {
    if (actual != expected)
    {
      throw new IllegalArgumentException(String.format(
        "%s() expects %d arguments, %d given",
        methodName, expected, actual));
    }
  }
}
