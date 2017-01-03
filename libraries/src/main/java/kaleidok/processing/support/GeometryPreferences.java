package kaleidok.processing.support;

import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import kaleidok.javafx.stage.AbstractGeometryPreferences;
import kaleidok.newt.event.AbstractWindowListener;
import processing.core.PApplet;
import processing.core.PConstants;


public class GeometryPreferences extends AbstractGeometryPreferences<Void>
{
  public double minDimension = 50;


  public GeometryPreferences( PApplet sketch, boolean hideProperties )
  {
    super(sketch, hideProperties, POSITION | SIZE);
  }


  public void applySize()
  {
    double w = this.w.get(), h = this.h.get();
    if (!Double.isNaN(w) && !Double.isNaN(h))
    {
      ((PApplet) getParent()).size(
        (int) Math.max(w, minDimension),
        (int) Math.max(h, minDimension));
    }
  }


  public void applyPosition()
  {
    PApplet sketch = (PApplet) getParent();
    if (PConstants.P3D.equals(sketch.sketchRenderer()))
      applyPosition((Window) sketch.getSurface().getNative());
  }


  public void applyPosition( Window window )
  {
    double x = this.x.get(), y = this.y.get();
    if (!Double.isNaN(x) && !Double.isNaN(y))
    {
      int ix = (int) x, iy = (int) y;
      RectangleImmutable intersection =
        window.getScreen().getViewport().intersection(
          ix, iy, ix + window.getWidth(), iy + window.getWidth());
      if (Math.min(intersection.getWidth(), intersection.getHeight()) >= minDimension)
        window.setPosition(ix, iy);
    }
  }


  @Override
  public void applyGeometry( Void ignored )
  {
    applyPosition();
    applySize();
  }


  @Override
  public void bind( Void ignored )
  {
    bind();
  }

  public void bind()
  {
    PApplet sketch = (PApplet) getParent();
    if (PConstants.P3D.equals(sketch.sketchRenderer()))
      bind((Window) sketch.getSurface().getNative());
  }


  public void bind( Window window )
  {
    BindingWindowListener bindings = new BindingWindowListener(window);
    window.addWindowListener(bindings);
    x.bind(bindings.xBinding);
    y.bind(bindings.yBinding);
    w.bind(bindings.wBinding);
    h.bind(bindings.hBinding);
  }


  private final class BindingWindowListener extends AbstractWindowListener
  {
    public final DoubleBinding xBinding, yBinding, wBinding, hBinding;


    public BindingWindowListener( final Window window )
    {
      this(
        Bindings.createDoubleBinding(
          () -> (double) (window.getX() + window.getScreen().getX())),
        Bindings.createDoubleBinding(
          () -> (double) (window.getY() + window.getScreen().getY())),
        Bindings.createDoubleBinding(
          () -> (double) window.getWidth()),
        Bindings.createDoubleBinding(
          () -> (double) window.getHeight()));
    }


    public BindingWindowListener(
      DoubleBinding xBinding, DoubleBinding yBinding,
      DoubleBinding wBinding, DoubleBinding hBinding )
    {
      this.xBinding = xBinding;
      this.yBinding = yBinding;
      this.wBinding = wBinding;
      this.hBinding = hBinding;
    }


    @Override
    public void windowMoved( WindowEvent ev )
    {
      xBinding.invalidate();
      yBinding.invalidate();
    }


    @Override
    public void windowResized( WindowEvent ev )
    {
      wBinding.invalidate();
      hBinding.invalidate();
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void windowDestroyed( WindowEvent ev )
    {
      x.unbind();
      y.unbind();
      w.unbind();
      h.unbind();
    }
  }
}
