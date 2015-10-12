package com.getflourish.stt2;

import processing.core.PApplet;


public class RecorderIcon
{
  protected final PApplet p;

  protected final STT stt;


  public float radius = 20;

  public float x = -10 - radius, y = -x;

  public float strokeWeight = 1;

  public int fillColor = 0xffff0000, strokeColor = 0xc0ffffff;


  public RecorderIcon( PApplet p, STT stt )
  {
    this.p = p;
    this.stt = stt;

    p.registerMethod("draw", this);
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
