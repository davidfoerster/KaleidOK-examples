package kaleidok.swing;

import java.awt.GraphicsDevice;
import java.awt.Window;
import java.util.EventListener;


public interface FullscreenEventListener extends EventListener
{
  void handleFullscreenStateChange( GraphicsDevice device, Window w,
    boolean fullscreen );
}
