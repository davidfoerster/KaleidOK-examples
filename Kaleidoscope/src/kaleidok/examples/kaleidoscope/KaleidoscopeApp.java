package kaleidok.examples.kaleidoscope;

import kaleidok.processing.AppletLauncher;
import kaleidok.processing.FudgedAppletViewerFactory;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.util.AssertionUtils;

import javax.swing.JApplet;
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
