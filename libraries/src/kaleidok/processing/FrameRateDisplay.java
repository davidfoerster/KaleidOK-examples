package kaleidok.processing;

import kaleidok.util.DefaultValueParser;
import kaleidok.util.Strings;
import processing.core.PApplet;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.ceil;
import static java.lang.Math.log10;
import static kaleidok.util.Math.clamp;


@SuppressWarnings("unused")
public class FrameRateDisplay extends Plugin<PApplet>
{
  public float offsetX = 4, offsetY = 4, textSize = 8;

  public int textColor = 0xff00ff00;


  private int frameRateSampleFrequencyMask = 0xf;

  private final char[] sampledFrameRate = new char[19];

  private int sampledFrameRateLength;


  private final char[] frameDrawTime = new char[19];

  private long drawStartTime;

  public TimeUnit timeUnit = TimeUnit.MILLISECONDS;


  public FrameRateDisplay( PApplet sketch )
  {
    super(sketch);
    sampledFrameRate[0] = '0';
    sampledFrameRateLength = 1;
  }


  public static FrameRateDisplay fromConfiguration( ExtPApplet sketch )
  {
    return
      DefaultValueParser.parseBoolean(
        sketch.getParameter("framerate.display"), false)
      ?
        new FrameRateDisplay(sketch) :
        null;
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


  @Override
  public void pre()
  {
    drawStartTime = System.nanoTime();
  }


  @Override
  public void draw()
  {
    final PApplet p = this.p;
    p.textSize(textSize);
    p.fill(textColor);

    if ((p.frameCount & frameRateSampleFrequencyMask) == 0) {
      sampledFrameRateLength =
        clamp((int) ceil(log10(p.frameRate)), 1, sampledFrameRate.length);
      Strings.toDigits((long) p.frameRate, 10,
        sampledFrameRate, 0, sampledFrameRateLength);
    }
    p.text(sampledFrameRate, 0, sampledFrameRateLength, offsetX, textSize + offsetY);

    long drawTime =
      timeUnit.convert(System.nanoTime() - drawStartTime, TimeUnit.NANOSECONDS);
    int drawTimeLength =
      clamp((int) ceil(log10(drawTime)), 1, frameDrawTime.length);
    p.text(Strings.toDigits(drawTime, 10, frameDrawTime, 0, drawTimeLength),
      0, drawTimeLength, offsetX, 2 * textSize + offsetY);
  }
}
