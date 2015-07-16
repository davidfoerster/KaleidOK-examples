package kaleidok.processing;

import kaleidok.util.Strings;
import processing.core.PApplet;

import java.util.concurrent.TimeUnit;


public class FrameRateDisplay
{
  private final PApplet p;

  public int offsetX = 4, textSize = 8, textColor = 0xff00ff00;


  private int frameRateSampleFrequencyMask = 0xf;

  private final char[] sampledFrameRate = new char[19];

  private int sampledFrameRateLength;


  private final char[] frameDrawTime = new char[19];

  private long drawStartTime;

  public TimeUnit timeUnit = TimeUnit.MILLISECONDS;


  public FrameRateDisplay( PApplet p )
  {
    sampledFrameRate[0] = '0';
    sampledFrameRateLength = 1;

    this.p = p;
    p.registerMethod("pre", this);
    p.registerMethod("draw", this);
  }


  public int getFrameRateSampleFrequency()
  {
    return frameRateSampleFrequencyMask + 1;
  }

  public void setFrameRateSampleFrequency( int frameRateSampleFrequency )
  {
    frameRateSampleFrequencyMask =
      Integer.highestOneBit(frameRateSampleFrequency) - 1;
  }


  public void pre()
  {
    drawStartTime = System.nanoTime();
  }


  public void draw()
  {
    p.textSize(textSize);
    p.fill(textColor);

    if ((p.frameCount & frameRateSampleFrequencyMask) == 0) {
      sampledFrameRateLength =
        Math.max(1, Math.min(sampledFrameRate.length,
          (int) Math.ceil(Math.log10(p.frameRate))));
      Strings.toDigits((long) p.frameRate, 10,
        sampledFrameRate, 0, sampledFrameRateLength);
    }
    p.text(sampledFrameRate, 0, sampledFrameRateLength, offsetX, textSize + offsetX);

    long drawTime =
      timeUnit.convert(System.nanoTime() - drawStartTime, TimeUnit.NANOSECONDS);
    int drawTimeLength =
      Math.max(1, Math.min(frameDrawTime.length,
        (int) Math.ceil(Math.log10(drawTime))));
    p.text(Strings.toDigits(drawTime, 10, frameDrawTime, 0, drawTimeLength),
      0, drawTimeLength, offsetX, 2 * textSize + offsetX);
  }
}
