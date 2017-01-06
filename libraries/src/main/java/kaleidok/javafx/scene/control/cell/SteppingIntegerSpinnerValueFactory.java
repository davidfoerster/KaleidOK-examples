package kaleidok.javafx.scene.control.cell;

import javafx.event.ActionEvent;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.Objects;

import static kaleidok.util.Math.clamp;


public class SteppingIntegerSpinnerValueFactory
  extends SpinnerValueFactory.IntegerSpinnerValueFactory
{
  public interface StepFunction
  {
    int increment( int currentValue, int steps, int stepSize );


    default int decrement( int currentValue, int steps, int stepSize )
    {
      return increment(currentValue, -steps, stepSize);
    }


    int coerceToStep( int x );
  }


  public StepFunction stepFunction;


  public SteppingIntegerSpinnerValueFactory( int min, int max )
  {
    this(min, max, null);
  }


  public SteppingIntegerSpinnerValueFactory( int min, int max,
    StepFunction stepFunction )
  {
    this(min, max, min, stepFunction);
  }


  public SteppingIntegerSpinnerValueFactory( int min, int max,
    int initialValue )
  {
    this(min, max, initialValue, null);
  }


  public SteppingIntegerSpinnerValueFactory( int min, int max,
    int initialValue, StepFunction stepFunction )
  {
    super(min, max, initialValue);
    this.stepFunction = stepFunction;
  }


  public void addTo( Spinner<Integer> spinner )
  {
    spinner.setValueFactory(this);
    spinner.getEditor().setOnAction(
      SteppingIntegerSpinnerValueFactory::handleEditorActionEvent);
  }


  private static void handleEditorActionEvent( ActionEvent ev )
  {
    TextField textField = (TextField) ev.getSource();
    @SuppressWarnings("unchecked")
    SpinnerValueFactory<Integer> valueFactory =
      ((Spinner<Integer>) textField.getParent()).getValueFactory();
    if (valueFactory != null)
    {
      StringConverter<Integer> converter = valueFactory.getConverter();
      if (converter != null)
      {
        String text = textField.getText();
        Integer value = converter.fromString(text);
        Integer coercedValue =
          (valueFactory instanceof SteppingIntegerSpinnerValueFactory) ?
            ((SteppingIntegerSpinnerValueFactory) valueFactory).coerceValue(value) :
            value;
        valueFactory.setValue(coercedValue);
        if (!Objects.equals(value, coercedValue))
          textField.setText(converter.toString(coercedValue));
      }
    }
  }


  private int coerceValue( int value )
  {
    StepFunction stepFunction = this.stepFunction;
    return (stepFunction != null) ? stepFunction.coerceToStep(value) : value;
  }


  private Integer coerceValue( Integer value )
  {
    if (value == null)
      return null;

    int coercedValue = coerceValue(value.intValue());
    return (coercedValue == value) ? value : Integer.valueOf(coercedValue);
  }


  public void setValueCoerced( Integer value )
  {
    setValue(coerceValue(value));
  }


  @Override
  public void decrement( int steps )
  {
    StepFunction stepFunction = this.stepFunction;
    if (stepFunction != null)
    {
      setValue(clamp(
        stepFunction.decrement(getValue(), steps, getAmountToStepBy()),
        getMin(), getMax()));
    }
    else
    {
      super.decrement(steps);
    }
  }


  @Override
  public void increment( int steps )
  {
    StepFunction stepFunction = this.stepFunction;
    if (stepFunction != null)
    {
      setValue(clamp(
        stepFunction.increment(getValue(), steps, getAmountToStepBy()),
        getMin(), getMax()));
    }
    else
    {
      super.increment(steps);
    }
  }


  public static class BinaryLogarithmStepFunction implements StepFunction
  {
    public static final BinaryLogarithmStepFunction INSTANCE =
      new BinaryLogarithmStepFunction();


    protected BinaryLogarithmStepFunction() { }


    @Override
    public int increment( int x, int shift, int stepSize )
    {
      if (stepSize != 1)
        throw new UnsupportedOperationException("Step size must be 1");

      int shiftDir = 1;
      if (shift <= 0)
      {
        if (shift == 0)
          return coerceToStep(x);

        shiftDir = -shiftDir;
        shift = -shift;
        x = -x;
      }

      int xLog2 =
        (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(Math.abs(x));
      if (x < 0)
      {
        if (xLog2 >= shift)
          return -shiftDir << (xLog2 - shift);

        // x = 0;
        shift -= xLog2 + 1;
        xLog2 = -1;
        if (shift == 0)
          return 0;
      }

      @SuppressWarnings("ConditionalExpressionWithIdenticalBranches")
      int shiftMax = (shiftDir >= 0) ? (Integer.SIZE - 2) : (Integer.SIZE - 1);
      return shiftDir << Math.min(xLog2 + shift, shiftMax);
    }


    @Override
    public int coerceToStep( int x )
    {
      return (x >= 0) ? Integer.highestOneBit(x) : -Integer.highestOneBit(-x);
    }
  }
}
