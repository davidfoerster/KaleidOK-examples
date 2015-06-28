package kaleidok.swing;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;


public class FullscreenAction extends AbstractAction
{
  public final Mode mode;


  protected FullscreenAction( Mode mode )
  {
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


  public enum Mode { TOGGLE, SET, UNSET }


  private static final FullscreenAction[] INSTANCES = new FullscreenAction[3];

  public static FullscreenAction getInstance( Mode mode )
  {
    int idx = mode.ordinal();
    if (INSTANCES[idx] == null)
      INSTANCES[idx] = new FullscreenAction(mode);
    return INSTANCES[idx];
  }

}
