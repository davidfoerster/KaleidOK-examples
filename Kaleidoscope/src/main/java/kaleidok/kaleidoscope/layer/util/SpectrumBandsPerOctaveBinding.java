package kaleidok.kaleidoscope.layer.util;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import kaleidok.audio.processor.MinimFFTProcessor;

import java.util.Objects;

import static java.lang.Math.ceil;
import static kaleidok.util.Math.log2;
import static kaleidok.util.Math.toIntExact;


public class SpectrumBandsPerOctaveBinding extends IntegerBinding
{
  public static final int MIN_FREQUENCY = 86;


  private final double octaveCount;

  public final ObservableIntegerValue totalBands;


  public SpectrumBandsPerOctaveBinding( ObservableIntegerValue totalBands,
    double sampleRate )
  {
    if (sampleRate <= 0 || !Double.isFinite(sampleRate))
      throw new IllegalArgumentException("non-positive or non-finite sample rate");

    bind(totalBands);
    this.totalBands = totalBands;
    octaveCount = log2(sampleRate * (0.5 / MIN_FREQUENCY));
  }


  @Override
  protected int computeValue()
  {
    return toIntExact(ceil(totalBands.get() / octaveCount));
  }


  @Override
  public void dispose()
  {
    unbind(totalBands);
  }


  public ChangeListener<Number> attach( MinimFFTProcessor processor )
  {
    MinimFFTProcessorChangeListener listener =
      new MinimFFTProcessorChangeListener(processor);
    listener.apply(get());
    addListener(listener);
    return listener;
  }


  private static class MinimFFTProcessorChangeListener
    implements ChangeListener<Number>
  {
    public final MinimFFTProcessor processor;


    public MinimFFTProcessorChangeListener( MinimFFTProcessor processor )
    {
      this.processor = Objects.requireNonNull(processor);
    }


    @Override
    public void changed( ObservableValue<? extends Number> observable,
      Number oldValue, Number newValue )
    {
      apply(newValue.intValue());
    }


    public void apply( int value )
    {
      processor.logAverages(MIN_FREQUENCY, value);
    }
  }
}
