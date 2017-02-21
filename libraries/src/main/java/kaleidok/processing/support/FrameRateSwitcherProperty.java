package kaleidok.processing.support;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.FPSAnimator;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedDoubleProperty;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedDoubleTag;
import kaleidok.javafx.util.converter.DoubleNumberStringConverter;
import kaleidok.processing.ExtPApplet;
import kaleidok.text.InternationalSystemOfUnitsFormat;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static kaleidok.util.Math.FLOAT_NAN_INT_BITS;


public class FrameRateSwitcherProperty extends AspectedDoubleProperty
{
  private final AtomicInteger changeRequest =
    new AtomicInteger(FLOAT_NAN_INT_BITS);


  public FrameRateSwitcherProperty( PApplet applet )
  {
    super(Objects.requireNonNull(applet), "target frame rate", 0);
    DoubleSpinnerValueFactory bounds =
      new DoubleSpinnerValueFactory(0, Float.MAX_VALUE);
    bounds.setAmountToStepBy(5);
    bounds.setConverter(new DoubleNumberStringConverter(
      InternationalSystemOfUnitsFormat.getNumberInstance(" Hz")));
    addAspect(BoundedDoubleTag.getDoubleInstance(), bounds);
  }


  @Override
  protected void invalidated()
  {
    changeRequest.set(Float.floatToIntBits(floatValue()));
  }


  public void setup()
  {
    PApplet applet = (PApplet) getBean();
    float targetFps = Float.intBitsToFloat(
      changeRequest.getAndSet(FLOAT_NAN_INT_BITS));

    if ((targetFps <= 0 || Float.isNaN(targetFps)) &&
      applet instanceof ExtPApplet)
    {
      String sFrameRate =
        ((ExtPApplet) applet).getParameterMap().get("framerate");
      if (sFrameRate != null)
        targetFps = Float.parseFloat(sFrameRate);
    }

    if (targetFps > 0)
    {
      applet.frameRate(targetFps);
    }
    else if (PConstants.P3D.equals(applet.sketchRenderer()))
    {
      GLAutoDrawable nativeSurface =
        (GLAutoDrawable) applet.getSurface().getNative();
      set(((FPSAnimator) nativeSurface.getAnimator()).getFPS());
    }
    else
    {
      PApplet.showVariationWarning("Setting the default initial frame rate");
    }

    applet.registerMethod("pre", this);
    applet.registerMethod("dispose", this);
  }


  public void pre()
  {
    int targetFpsBits = changeRequest.getAndSet(FLOAT_NAN_INT_BITS);
    if (targetFpsBits != FLOAT_NAN_INT_BITS)
      ((PApplet) getBean()).frameRate(Float.intBitsToFloat(targetFpsBits));
  }


  public void dispose()
  {
    PApplet applet = (PApplet) getBean();
    applet.unregisterMethod("pre", this);
    applet.unregisterMethod("dispose", this);
  }
}
