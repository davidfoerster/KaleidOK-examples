package kaleidok.processing;

import kaleidok.util.DebugManager;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

import javax.swing.JApplet;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private JApplet parent;

  public ExtPApplet( JApplet parent )
  {
    this.parent = parent;
    if (parent != null)
      DebugManager.fromApplet(parent);
  }

  @Override
  public String getParameter( String name )
  {
    return (parent != null) ? parent.getParameter(name) : null;
  }

  public Object getParameter( String name, Object defaultValue )
  {
    return DefaultValueParser.parse(getParameter(name), defaultValue);
  }

  @Override
  public void keyPressed( KeyEvent e )
  {
    for (KeyListener listener: getKeyListeners()) {
      if (listener != this) {
        listener.keyPressed(e);
        if (e.isConsumed())
          return;
      }
    }
    super.keyPressed(e);
  }

  @Override
  public void keyReleased( KeyEvent e )
  {
    super.keyReleased(e);
  }

  @Override
  public void keyTyped( KeyEvent e )
  {
    super.keyTyped(e);
  }
}
