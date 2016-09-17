package kaleidok.javafx.geometry;

import com.jogamp.nativewindow.util.RectangleImmutable;
import javafx.geometry.Rectangle2D;


public final class Rectangles
{
  private Rectangles() { }


  public static Rectangle2D from( RectangleImmutable r )
  {
    return new Rectangle2D(r.getX(), r.getY(), r.getWidth(), r.getHeight());
  }


  public static Rectangle2D union( Rectangle2D a, Rectangle2D b )
  {
    double
      x1 = Math.min(a.getMinX(), b.getMinX()),
      y1 = Math.min(a.getMinY(), b.getMinY()),
      x2 = Math.max(a.getMaxX(), b.getMaxX()),
      y2 = Math.max(a.getMaxY(), b.getMaxY());
    return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
  }
}
