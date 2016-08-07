package kaleidok.processing.event;

import com.jogamp.nativewindow.NativeSurfaceHolder;
import com.jogamp.newt.event.KeyEvent;
import jogamp.newt.awt.event.AWTNewtEventFactory;


public final class KeyEventSupport
{
  private KeyEventSupport() { }


  public static final NativeSurfaceHolder dummySurfaceHolder = () -> null;


  public static KeyEvent convert( processing.event.KeyEvent event )
  {
    Object nativeEvent = event.getNative();
    return
      (nativeEvent instanceof KeyEvent) ?
        (KeyEvent) nativeEvent :
      (nativeEvent instanceof java.awt.event.KeyEvent) ?
        AWTNewtEventFactory.createKeyEvent(
          (java.awt.event.KeyEvent) nativeEvent, dummySurfaceHolder) :
        null;
  }
}
