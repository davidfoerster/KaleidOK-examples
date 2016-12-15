package kaleidok.newt;

import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;

import java.util.Collections;


public final class WindowSupport
{
  private WindowSupport() { }


  public static boolean toggleFullscreen( Window w )
  {
    boolean newFullscreenState;
    newFullscreenState = w.isFullscreen() ?
      w.setFullscreen(false) :
      w.setFullscreen(Collections.singletonList(getMainMonitor(w)));
    return newFullscreenState;
  }


  /**
   * Re-implement {@link Window#getMainMonitor()} because it's broken on multi-
   * monitor setups. More specifically {@link Window#getBounds()} returns the
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
    return new Rectangle(
      w.getScreen().getX() + w.getX(), w.getScreen().getY() + w.getY(),
      w.getWidth(), w.getHeight());
  }
}
