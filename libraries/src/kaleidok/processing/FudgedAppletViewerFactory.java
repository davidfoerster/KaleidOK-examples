package kaleidok.processing;

import sun.applet.AppletViewer;
import sun.applet.AppletViewerFactory;

import java.awt.Frame;
import java.awt.Label;
import java.awt.MenuBar;
import java.net.URL;
import java.util.Hashtable;


public class FudgedAppletViewerFactory implements AppletViewerFactory
{
  @Override
  public AppletViewer createAppletViewer( int x, int y, URL doc,
    Hashtable parameters )
  {
    AppletViewer appletViewer =
      new AppletViewer(x, y, doc, parameters, System.out, this);
    fudgeAppletViewer(appletViewer);
    return appletViewer;
  }


  public static void fudgeAppletViewer( Frame frame )
  {
    synchronized (frame.getTreeLock()) {
      frame.setMenuBar(null);
      int len = frame.getComponentCount();
      for (int i = 0; i < len; i++) {
        if (frame.getComponent(i) instanceof Label) {
          frame.remove(i);
          break;
        }
      }
    }
  }


  @Override
  public MenuBar getBaseMenuBar()
  {
    return new MenuBar();
  }


  @Override
  public boolean isStandalone()
  {
    return true;
  }
}
