package kaleidok.kaleidoscope.layer;

import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.SimpleBoundedDoubleProperty;
import kaleidok.javafx.util.converter.NumberStringConverterBase;
import kaleidok.text.Numbers;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public final class LayerUtils
{
  private LayerUtils() { }


  public static SimpleBoundedDoubleProperty adjustFormat(
    SimpleBoundedDoubleProperty property, NumberFormat fmt )
  {
    DoubleSpinnerValueFactory svf = property.getBounds();
    StringConverter<Double> converter = svf.getConverter();

    if (fmt == null)
    {
      if (converter instanceof NumberStringConverterBase)
      {
        NumberStringConverterBase<Double> nsc =
          (NumberStringConverterBase<Double>) converter;
        fmt = nsc.getFormat();
        nsc.resetCaches();
      }
      else
      {
        fmt = NumberFormat.getNumberInstance();
      }
    }

    Numbers.adjustFractionalDigits(fmt, property.get(),
      svf.getMin(), svf.getMax(), svf.getAmountToStepBy());

    if (!(converter instanceof NumberStringConverterBase))
    {
      svf.setConverter(
        new NumberStringConverterBase.DoubleNumberStringConverter(fmt));
    }

    return property;
  }


  public static SimpleBoundedDoubleProperty adjustFormat(
    SimpleBoundedDoubleProperty property, String formatString )
  {
    return adjustFormat(property, new DecimalFormat(formatString));
  }


  public static SimpleBoundedDoubleProperty adjustFormat(
    SimpleBoundedDoubleProperty property )
  {
    return adjustFormat(property, (NumberFormat) null);
  }


  public static SimpleBoundedDoubleProperty adjustPercentFormat(
    SimpleBoundedDoubleProperty property )
  {
    return adjustFormat(property, NumberFormat.getPercentInstance());
  }


  public static SimpleBoundedDoubleProperty adjustPermilleFormat(
    SimpleBoundedDoubleProperty property )
  {
    String permille = "\u2030";
    DecimalFormat fmt = new DecimalFormat();
    fmt.setMultiplier(1000);
    fmt.setPositiveSuffix(permille);
    fmt.setNegativeSuffix(permille);
    return adjustFormat(property, fmt);
  }
}
