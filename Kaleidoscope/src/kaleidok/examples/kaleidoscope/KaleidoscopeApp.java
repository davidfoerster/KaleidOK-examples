package kaleidok.examples.kaleidoscope;

import kaleidok.processing.AppletLauncher;
import kaleidok.processing.FudgedAppletViewerFactory;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.util.AssertionUtils;

import javax.swing.JApplet;
import java.awt.*;
import java.io.IOException;


public class KaleidoscopeApp extends ProcessingSketchAppletWrapper<Kaleidoscope>
{
  private KaleidoscopeControls controls = null;


  @Override
  public void init()
  {
    sketchFactory = new PAppletFactory<Kaleidoscope>()
      {
        @Override
        public Kaleidoscope createInstance( JApplet parent ) throws InstantiationException
        {
          return new Kaleidoscope(parent);
        }
      };

    super.init();
  }


  @Override
  protected void initComponents()
  {
    KaleidoscopeControls controls = getControls();
    controls.setVisible(true);
  }


  public KaleidoscopeControls getControls()
  {
    if (controls == null) {
      controls = new KaleidoscopeControls(this);

      Window w = getRootPane().getTopLevelWindow();
      Rectangle screen = w.getGraphicsConfiguration().getBounds();
      int x = w.getX() + w.getWidth() + 5,
        y = w.getY() + w.getHeight() + 5;
      if (screen.x + screen.width >= x + controls.getWidth()) {
        controls.setLocation(x, w.getY());
      } else if (screen.y + screen.height >= y + controls.getHeight()) {
        controls.setLocation(w.getX(), y);
      }
    }
    return controls;
  }


  public static void main( String... args ) throws IOException
  {
    AssertionUtils.enableAssertionsOnDebugging();
    new AppletLauncher(new FudgedAppletViewerFactory())
      .launch(KaleidoscopeApp.class, args);
  }
}
