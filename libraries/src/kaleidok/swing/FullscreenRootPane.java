package kaleidok.swing;

import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collection;


public class FullscreenRootPane extends JRootPane
{
  private Window topLevelWindow = null;

  private Rectangle windowedBounds = null;

  private Collection<FullscreenEventListener> fullscreenListeners =
    new ArrayList<>();


  public Window getTopLevelWindow()
  {
    if (topLevelWindow == null) {
      topLevelWindow = getTopLevelWindow(this);
    } else {
      assert topLevelWindow == getTopLevelWindow(this);
    }
    return topLevelWindow;
  }

  private static Window getTopLevelWindow( Component c )
  {
    Container parent;
    while ((parent = c.getParent()) != null)
      c = parent;
    return (c instanceof Window) ? (Window) c : null;
  }


  public boolean isFullscreen()
  {
    Window w = getTopLevelWindow();
    return w != null && isFullscreen(w);
  }

  private static boolean isFullscreen( Window w )
  {
    GraphicsDevice dev = w.getGraphicsConfiguration().getDevice();
    return dev != null && w == dev.getFullScreenWindow();
  }


  public void toggleFullscreen()
  {
    moveToScreen(-1, !isFullscreen());
  }


  public void moveToScreen( int i, boolean fullscreen )
  {
    Window w = getTopLevelWindow();
    if (w == null)
      throw new IllegalComponentStateException(this + " has no top-level window.");

    if (i >= 0) {
      GraphicsDevice[] devs =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
      if (i < devs.length) {
        GraphicsDevice dev = devs[i];
        if (fullscreen) {
          setFullscreenWindow(dev, w, true);
        } else {
          GraphicsConfiguration config = dev.getDefaultConfiguration();
          if (!config.equals(w.getGraphicsConfiguration())) {
            w.setLocation(config.getBounds().getLocation());
          }
        }
        return;
      }

      System.err.println(
        "Warning: No screen with index " + i +
        " available; using default screen instead.");
    }

    // use default screen
    setFullscreenWindow(w.getGraphicsConfiguration().getDevice(), w, fullscreen);
  }


  private void setFullscreenWindow( GraphicsDevice dev, Window w, boolean fullscreen )
  {
    boolean previousFullscreenState = isFullscreen(w);
    if (!previousFullscreenState)
      windowedBounds = w.getBounds(windowedBounds);

    if (w instanceof Frame) {
      final Frame frame = (Frame) w;
      frame.removeNotify();
      frame.setUndecorated(fullscreen);
      frame.setResizable(!fullscreen);
      frame.addNotify();
      if (fullscreen) {
        frame.addFocusListener(FullscreenFocusListener.INSTANCE);
      } else {
        frame.removeFocusListener(FullscreenFocusListener.INSTANCE);
      }
    }

    dev.setFullScreenWindow(fullscreen ? w : null);
    getContentPane().requestFocusInWindow();

    for (FullscreenEventListener listener: fullscreenListeners)
      listener.handleFullscreenStateChange(dev, w, fullscreen);

    if (!fullscreen && previousFullscreenState && windowedBounds != null)
      w.setBounds(windowedBounds);
  }


  public boolean addFullscreenEventListener(
    FullscreenEventListener fullscreenFocusListener )
  {
    return (fullscreenFocusListener != null) &&
      fullscreenListeners.add(fullscreenFocusListener);
  }

  public boolean removeFullscreenEventListener(
    FullscreenEventListener fullscreenFocusListener )
  {
    return (fullscreenFocusListener != null) &&
      fullscreenListeners.remove(fullscreenFocusListener);
  }


  private static class FullscreenFocusListener implements FocusListener
  {
    public static final FullscreenFocusListener INSTANCE =
      new FullscreenFocusListener();

    protected FullscreenFocusListener() { }

    @Override
    public void focusGained( FocusEvent ev )
    {
      setAlwaysOnTop(ev, true);
    }

    @Override
    public void focusLost( FocusEvent ev )
    {
      setAlwaysOnTop(ev, false);
    }

    private void setAlwaysOnTop( FocusEvent ev, boolean alwaysOnTop )
    {
      ((Frame) ev.getSource()).setAlwaysOnTop(alwaysOnTop);
    }
  }


  public FullscreenAction registerKeyAction( FullscreenAction.Mode mode, KeyStroke keyStroke )
  {
    FullscreenAction action = new FullscreenAction(mode);
    action.registerKeyAction(this, WHEN_IN_FOCUSED_WINDOW, keyStroke);
    return action;
  }

}
