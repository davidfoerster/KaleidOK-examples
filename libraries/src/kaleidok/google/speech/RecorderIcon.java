package kaleidok.google.speech;

import javafx.beans.value.ObservableValue;
import kaleidok.google.speech.STT.State;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.Plugin;
import kaleidok.util.prefs.DefaultValueParser;
import processing.core.PApplet;
import processing.core.PConstants;


public class RecorderIcon extends Plugin<PApplet>
{
  protected final ObservableValue<State> recorderState;


  public float radius = 20;

  public float x = -10 - radius, y = -x;

  public float strokeWeight = 1;

  public int fillColor = 0xffff0000, strokeColor = 0xc0ffffff;


  public RecorderIcon( PApplet sketch, ObservableValue<State> recorderState )
  {
    super(sketch);
    this.recorderState = Objects.requireNonNull(recorderState);
  }


  public static RecorderIcon fromConfiguration( ExtPApplet sketch,
    ObservableValue<State> recorderState, boolean defaultOn )
  {
    String strEnabled =
      sketch.getParameterMap().getOrDefault(
        RecorderIcon.class.getCanonicalName() + ".enabled", "default");
    boolean enabled =
      "default".equals(strEnabled) ?
        defaultOn :
        DefaultValueParser.parseBoolean(strEnabled);
    return enabled ? new RecorderIcon(sketch, recorderState) : null;
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
}
