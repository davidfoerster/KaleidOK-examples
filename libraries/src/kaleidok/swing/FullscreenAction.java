package kaleidok.swing;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;


public class FullscreenAction extends AbstractAction
{
  public final Mode mode;

  // TODO: Cache instance for each mode

  public FullscreenAction( Mode mode )
  {
    checkMode(mode);
    this.mode = mode;
  }

  @Override
  public void actionPerformed( ActionEvent e )
  {
    FullscreenRootPane source = (FullscreenRootPane) e.getSource();
    switch (mode) {
    case TOGGLE:
      source.toggleFullscreen();
      break;

    case SET:
    case UNSET:
      source.moveToScreen(-1, mode == Mode.SET);
      break;
    }
  }

  public void registerKeyAction( FullscreenRootPane c, int condition, KeyStroke keyStroke )
  {
    c.getInputMap(condition).put(keyStroke, mode);
    c.getActionMap().put(mode, this);
  }

  private static void checkMode( Mode mode )
  {
    if (mode == null)
      throw new NullPointerException("mode");
  }


  public enum Mode { TOGGLE, SET, UNSET }

}
