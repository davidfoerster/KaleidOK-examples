package kaleidok.processing.event;

import com.jogamp.nativewindow.NativeSurfaceHolder;
import com.jogamp.newt.event.KeyEvent;
import jogamp.newt.awt.event.AWTNewtEventFactory;

import static com.jogamp.newt.event.KeyEvent.NULL_CHAR;
import static com.jogamp.newt.event.KeyEvent.VK_UNDEFINED;


public final class KeyEventSupport
{
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
