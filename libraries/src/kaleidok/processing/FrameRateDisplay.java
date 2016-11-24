package kaleidok.processing;

import javafx.beans.property.IntegerProperty;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedIntegerProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedIntegerTag;
import kaleidok.util.Strings;
import processing.core.PApplet;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Math.ceil;
import static java.lang.Math.log10;
import static kaleidok.util.Math.clamp;


/**
 * Displays the frame rate and frame rendering duration in a Processing sketch.
 */
public class FrameRateDisplay extends Plugin<PApplet>
  implements PreferenceBean
{
  private final AspectedIntegerProperty enabled;

  public float offsetX = 4, offsetY = 4, textSize = 8;

  public int textColor = 0xff00ff00;


  private int frameRateSampleFrequencyMask = 0xf;

  private final char[] sampledFrameRate = new char[19];

  {
    sampledFrameRate[0] = '0';
  }

  private int sampledFrameRateLength = 1;


  private final char[] frameDrawTime = new char[19];

  private long drawStartTime;

  public TimeUnit timeUnit = TimeUnit.MILLISECONDS;


  public FrameRateDisplay( PApplet sketch )
  {
    this(sketch, 1);
  }


  public FrameRateDisplay( PApplet sketch, int enabled )
  {
    super(sketch);

    IntegerSpinnerValueFactory bounds = new IntegerSpinnerValueFactory(0, 1);
    this.enabled =
      new AspectedIntegerProperty(this, "enabled",
        clamp(enabled, bounds.getMin(), bounds.getMax()));
    this.enabled.addAspect(BoundedIntegerTag.getIntegerInstance(), bounds);
    this.enabled.addAspect(PropertyPreferencesAdapterTag.getInstance());
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
    if (enabled.get() <= 0)
      return;

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


  @Override
  public String getName()
  {
    return "frame rate display";
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
