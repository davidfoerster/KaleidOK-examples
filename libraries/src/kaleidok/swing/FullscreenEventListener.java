package kaleidok.swing;

import java.awt.Window;
import java.util.EventListener;


public interface FullscreenEventListener extends EventListener
{
  void handleFullscreenStateChange( Window w, boolean fullscreen );
}
