package kaleidok.kaleidoscope;

import javafx.embed.swing.SwingNode;
import javafx.geometry.Side;
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
  public void start( Stage stage ) throws Exception
  {
    stage.setTitle("Kaleidoscope Controls");
    super.start(stage);
  }


  @SuppressWarnings("ProhibitedExceptionDeclared")
  @Override
  protected void show( Stage stage ) throws Exception
  {
    if (Double.isNaN(stage.getX()) || Double.isNaN(stage.getY()))
      placeAroundSketch(stage, 2, (Side[]) null);

    super.show(stage);
  }


  @Override
  protected PAppletFactory<Kaleidoscope> getSketchFactory()
  {
    return new SimplePAppletFactory<>(Kaleidoscope.class);
  }


  public synchronized KaleidoscopeControls getControls()
  {
    if (controls == null)
      controls = new KaleidoscopeControls(this);
    return controls;
  }


  public static void main( String... args )
  {
    AssertionUtils.enableAssertionsOnDebugging();
    launch(KaleidoscopeApp.class, args);
  }
}
