package kaleidok.newt;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;
import kaleidok.util.Arrays;


public final class WindowSupport
{
  private WindowSupport() { }


  public static boolean toggleFullscreen( Window w,
    PointImmutable desiredWindowLocation )
  {
    boolean newFullscreenState;
    if (w.isFullscreen())
    {
      newFullscreenState = w.setFullscreen(false);
      w.setPosition(desiredWindowLocation.getX(), desiredWindowLocation.getY());
    }
    else
    {
      newFullscreenState =
        w.setFullscreen(Arrays.asImmutableList(getMainMonitor(w)));
    }
    return newFullscreenState;
  }


  /**
   * Re-implement {@link Window#getMainMonitor()} because it's broken on multi-
   * monitor setups. More importantly {@link Window#getBounds()} returns the
   * position on the current monitor and not on the entire screen space.
   *
   * @param w  A window
   * @return  The monitor with the highest viewport coverage of the window
   * @see Window#getMainMonitor()
   */
  public static MonitorDevice getMainMonitor( Window w )
  {
    return w.getScreen().getMainMonitor(getBoundsOnScreen(w));
  }


  public static Rectangle getBoundsOnScreen( Window w )
  {
    Point windowLocation = w.getLocationOnScreen(null);
    return new Rectangle(
      windowLocation.getX(), windowLocation.getY(),
      w.getWidth(), w.getHeight());
  }
}
