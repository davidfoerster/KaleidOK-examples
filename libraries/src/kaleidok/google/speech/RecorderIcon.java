package kaleidok.google.speech;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.Plugin;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;


public class RecorderIcon extends Plugin<PApplet>
{
  protected final STT stt;


  public float radius = 20;

  public float x = -10 - radius, y = -x;

  public float strokeWeight = 1;

  public int fillColor = 0xffff0000, strokeColor = 0xc0ffffff;


  public RecorderIcon( PApplet sketch, STT stt )
  {
    super(sketch);
    this.stt = stt;
  }


  public static RecorderIcon fromConfiguration( ExtPApplet sketch, STT stt,
    boolean defaultOn )
  {
    String strEnabled =
      sketch.getParameter(RecorderIcon.class.getCanonicalName() + ".enabled");
    return
      (!"forceoff".equals(strEnabled) &&
        (defaultOn || DefaultValueParser.parseBoolean(strEnabled, true)))
      ?
        new RecorderIcon(sketch, stt) :
        null;
  }


  public void draw()
  {
    PApplet p = this.p;
    switch (stt.getStatus()) {
    case RECORDING:
      int previousEllipseMode = p.g.ellipseMode;
      p.ellipseMode(PApplet.RADIUS);
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
      break;
    }
  }
}
