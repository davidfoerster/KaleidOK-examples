package kaleidok.image.filter;

import kaleidok.util.function.BinaryFloatFunction;

import java.awt.image.RGBImageFilter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;


public final class Parser
{
  public static final Pattern IMAGE_FILTER_SPEC_PATTERN = Pattern.compile(
    "^(?<operator>[*/+-]?)\\s*(?<colormodel>\\w+)\\s*\\(\\s*(?<args>[^\\s,)]+(?:\\s*,\\s*[^\\s,)]+)*)\\s*\\)",
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
    char operatorChar =
      (operatorLen == 1) ? s.charAt(operatorStart) :
      (operatorLen == 0) ? '+' :
        '\0';
    BinaryFloatFunction operator;
    switch (operatorChar)
    {
    case '+':
      operator = (a, b) -> a + b;
      break;

    case '-':
      operator = (a, b) -> a - b;
      break;

    case '*':
      operator = (a, b) -> a * b;
      break;

    case '/':
      operator = (a, b) -> a / b;
      break;

    default:
      throw new AssertionError("Unexpected operator");
    }

    switch (m.group("colormodel").toLowerCase(Locale.ROOT))
    {
    case "hsb":
      if (args.length != 3)
      {
        throw new IllegalArgumentException(
          "hsb() expects 3 arguments, " + args.length + " given");
      }
      return new HSBAdjustFilter(
        (float) args[0], (float) args[1], (float) args[2], operator);

    case "rgb":
    default:
      throw new UnsupportedOperationException(
        "Unknown or unimplemented color model: " + m.group("colormodel"));
    }
  }
}
