package kaleidok.newt;

import com.jogamp.newt.Window;


public final class WindowSupport
{
  private WindowSupport() { }


  public static boolean toggleFullscreen( Window w )
  {
    return w.setFullscreen(!w.isFullscreen());
  }
}
