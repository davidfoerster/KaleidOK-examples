package kaleidok.swing;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public abstract class KeyStrokeListener implements KeyListener
{
  public KeyStroke stroke;


  public KeyStrokeListener( KeyStroke stroke )
  {
    this.stroke = stroke;
  }


  @Override
  public void keyTyped( KeyEvent ev )
  {
    filterKeyEvent(ev);
  }

  @Override
  public void keyPressed( KeyEvent ev )
  {
    filterKeyEvent(ev);
  }

  @Override
  public void keyReleased( KeyEvent ev )
  {
    filterKeyEvent(ev);
  }


  private void filterKeyEvent( KeyEvent ev )
  {
    if (matchKeyStroke(ev) && matchModifiers(ev))
      handleKey(ev);
  }

  private boolean matchKeyStroke( KeyEvent ev )
  {
    return (ev.getID() == stroke.getKeyEventType()) &&
      ((ev.getID() == KeyEvent.KEY_TYPED) ?
        ev.getKeyChar() == stroke.getKeyChar() :
        ev.getKeyCode() == stroke.getKeyCode());
  }

  private boolean matchModifiers( KeyEvent ev )
  {
    return (ev.getModifiers() | ev.getModifiersEx()) == stroke.getModifiers();
  }


  protected abstract void handleKey( KeyEvent ev );
}
