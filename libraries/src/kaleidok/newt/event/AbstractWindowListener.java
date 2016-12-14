package kaleidok.newt.event;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;


public class AbstractWindowListener implements WindowListener
{
  @Override
  public void windowResized( WindowEvent e ) { }

  @Override
  public void windowMoved( WindowEvent e ) { }

  @Override
  public void windowDestroyNotify( WindowEvent e ) { }

  @Override
  public void windowDestroyed( WindowEvent e ) { }

  @Override
  public void windowGainedFocus( WindowEvent e ) { }

  @Override
  public void windowLostFocus( WindowEvent e ) { }

  @Override
  public void windowRepaint( WindowUpdateEvent e ) { }
}
