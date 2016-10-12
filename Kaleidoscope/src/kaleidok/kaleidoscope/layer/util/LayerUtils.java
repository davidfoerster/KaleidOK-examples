package kaleidok.kaleidoscope.layer.util;

import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.util.converter.NumberStringConverterBase;
import kaleidok.text.Numbers;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public final class LayerUtils
{
  private LayerUtils() { }


  public static AspectedDoubleProperty adjustFormat(
    AspectedDoubleProperty property, NumberFormat fmt )
  {
    DoubleSpinnerValueFactory svf =
      property.getAspect(BoundedDoubleTag.INSTANCE);
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


  public static AspectedDoubleProperty adjustFormat(
    AspectedDoubleProperty property, String formatString )
  {
    return adjustFormat(property, new DecimalFormat(formatString));
  }


  public static AspectedDoubleProperty adjustFormat(
    AspectedDoubleProperty property )
  {
    return adjustFormat(property, (NumberFormat) null);
  }


  public static AspectedDoubleProperty adjustPercentFormat(
    AspectedDoubleProperty property )
  {
    return adjustFormat(property, NumberFormat.getPercentInstance());
  }


  public static AspectedDoubleProperty adjustPermilleFormat(
    AspectedDoubleProperty property )
  {
    String permille = "\u2030";
    DecimalFormat fmt = new DecimalFormat();
    fmt.setMultiplier(1000);
    fmt.setPositiveSuffix(permille);
    fmt.setNegativeSuffix(permille);
    return adjustFormat(property, fmt);
  }
}
