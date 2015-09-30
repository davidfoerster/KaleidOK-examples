package com.getflourish.stt2;

import processing.core.PApplet;


public class RecorderIcon
{
  protected final PApplet p;

  protected final STT stt;


  public float radius = 20;

  public float
    x = 10 + radius,
    y = 10 + radius;

  public float strokeWeight = 1;

  public int fillColor = 0xffff0000, strokeColor = 0xffffffff;


  public RecorderIcon( PApplet p, STT stt )
  {
    this.p = p;
    this.stt = stt;

    p.registerMethod("draw", this);
  }


  public void draw()
  {
    switch (stt.getStatus()) {
    case RECORDING:
      p.fill(fillColor);
      p.stroke(strokeColor);
      p.strokeWeight(strokeWeight);

      p.ellipseMode(PApplet.RADIUS);
      p.ellipse(x, y, radius, radius);
      p.ellipseMode(PApplet.CENTER);
    }
  }
}
