package kaleidok.kaleidoscope;

import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchApplication;
import kaleidok.processing.SimplePAppletFactory;
import kaleidok.util.AssertionUtils;

import java.awt.Dimension;


public class KaleidoscopeApp extends ProcessingSketchApplication<Kaleidoscope>
{
  private Scene scene;

  private KaleidoscopeControls controls = null;


  @Override
  public void init() throws Exception
  {
    super.init();
  }


  @Override
  protected synchronized Scene getScene()
  {
    if (scene == null)
    {
      KaleidoscopeControls controls = getControls();
      Dimension size = controls.getPreferredSize();

      SwingNode swingNode = new SwingNode();
      swingNode.setContent(controls);

      StackPane pane = new StackPane(swingNode);
      scene = new Scene(pane, size.getWidth(), size.getHeight());
    }
    return scene;
  }


  @Override
  public void start( Stage stage )
  {
    stage.setTitle("Kaleidoscope Controls");
    super.start(stage);
  }


  @Override
  protected PAppletFactory<Kaleidoscope> getSketchFactory()
  {
    return new SimplePAppletFactory<>(Kaleidoscope.class);
  }


  public synchronized KaleidoscopeControls getControls()
  {
    if (controls == null) {
      controls = new KaleidoscopeControls(this);

      /*
      Window w = getRootPane().getTopLevelWindow();
      Rectangle screen = w.getGraphicsConfiguration().getBounds();
      int x = w.getX() + w.getWidth() + 5,
        y = w.getY() + w.getHeight() + 5;
      if (screen.x + screen.width >= x + controls.getWidth()) {
        controls.setLocation(x, w.getY());
      } else if (screen.y + screen.height >= y + controls.getHeight()) {
        controls.setLocation(w.getX(), y);
      }
      */
    }
    return controls;
  }


  public static void main( String... args )
  {
    AssertionUtils.enableAssertionsOnDebugging();
    launch(KaleidoscopeApp.class, args);
  }
}
