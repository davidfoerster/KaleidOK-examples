package kaleidok.javafx.stage;

import javafx.application.Application;
import javafx.stage.Window;


public class GeometryPreferences extends AbstractGeometryPreferences<Window, Application>
{
  public GeometryPreferences( Application parent, boolean hideProperties,
    int mode )
  {
    super(parent, hideProperties, mode);
  }


  public GeometryPreferences( Application parent, Class<?> beanClass,
    boolean hideProperties, int mode )
  {
    super(parent, beanClass, hideProperties, mode);
  }


  @Override
  public void applyGeometry( Window window )
  {
    if (this.x != null && this.y != null)
    {
      double x = this.x.get(), y = this.y.get();
      if (!Double.isNaN(x) && !Double.isNaN(y))
      {
        window.setX(x);
        window.setY(y);
      }
    }

    if (this.w != null && this.h != null)
    {
      double w = this.w.get(), h = this.h.get();
      if (!Double.isNaN(w) && !Double.isNaN(h))
      {
        window.setWidth(w);
        window.setHeight(h);
      }
    }
  }


  @Override
  public void bind( Window window )
  {
    if (this.x != null && this.y != null)
    {
      x.bind(window.xProperty());
      y.bind(window.yProperty());
    }
    if (this.w != null && this.h != null)
    {
      w.bind(window.widthProperty());
      h.bind(window.heightProperty());
    }
    if (this.show != null)
      show.bind(window.showingProperty());
  }
}
