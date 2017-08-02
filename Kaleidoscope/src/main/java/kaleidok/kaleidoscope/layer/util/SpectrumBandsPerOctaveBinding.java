package kaleidok.kaleidoscope.layer.util;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
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
    return toIntExact(ceil(totalBands.doubleValue() / octaveCount));
  }


  @Override
  public void dispose()
  {
    unbind(totalBands);
  }


  @SuppressWarnings("UnusedReturnValue")
  public ChangeListener<Number> attach( final MinimFFTProcessor processor )
  {
    Objects.requireNonNull(processor);
    ChangeListener<Number> listener =
      ( obs, oldValue, newValue ) ->
        processor.setAverageParams(
          MinimFFTProcessor.AverageType.LOGARITHMIC, MIN_FREQUENCY,
          newValue.intValue());

    listener.changed(this, null, getValue());
    addListener(listener);
    return listener;
  }
}
