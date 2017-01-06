package kaleidok.google.speech;

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.google.speech.STT.State;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.processing.Plugin;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.Objects;
import java.util.stream.Stream;

import static kaleidok.util.Math.clamp;


public class RecorderIcon extends Plugin<PApplet> implements PreferenceBean
{
  protected final ObservableValue<State> recorderState;

  private final AspectedIntegerProperty enabled;

  public float radius = 20;

  public float x = -10 - radius, y = -x;

  public float strokeWeight = 1;

  public int fillColor = 0xffff0000, strokeColor = 0xc0ffffff;


  public RecorderIcon( PApplet sketch, ObservableValue<State> recorderState )
  {
    this(sketch, recorderState, 1);
  }


  public RecorderIcon( PApplet sketch, ObservableValue<State> recorderState,
    int enabled )
  {
    super(sketch);
    this.recorderState = Objects.requireNonNull(recorderState);

    IntegerSpinnerValueFactory bounds = new IntegerSpinnerValueFactory(0, 1);
    this.enabled =
      new AspectedIntegerProperty(this, "enabled",
        clamp(enabled, bounds.getMin(), bounds.getMax()));
    this.enabled.addAspect(BoundedIntegerTag.getIntegerInstance(), bounds);
    this.enabled.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  @Override
  public void draw()
  {
    if (enabled.get() <= 0)
      return;

    PApplet p = this.p;
    if (recorderState.getValue() == State.RECORDING)
    {
      int previousEllipseMode = p.g.ellipseMode;
      p.ellipseMode(PConstants.RADIUS);
      p.fill(fillColor);
      p.stroke(strokeColor);
      p.strokeWeight(strokeWeight);

      float x = this.x, y = this.y, radius = this.radius;
      if (x < 0)
        x += p.width;
      if (y < 0)
        y += p.height;
      p.ellipse(x, y, radius, radius);
      p.ellipseMode(previousEllipseMode);
    }
  }


  @Override
  public String getName()
  {
    return "recording icon";
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return Stream.of(enabled.getAspect(
      PropertyPreferencesAdapterTag.getWritableInstance()));
  }


  public IntegerProperty enabledProperty()
  {
    return enabled;
  }

  public int getEnabled()
  {
    return enabled.get();
  }

  public void setEnabled( int value )
  {
    enabled.set(value);
  }
}
