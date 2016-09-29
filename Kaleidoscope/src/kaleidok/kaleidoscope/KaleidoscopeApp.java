package kaleidok.kaleidoscope;

import javafx.embed.swing.SwingNode;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kaleidok.javafx.stage.Icons;
import kaleidok.kaleidoscope.controls.KaleidoscopeConfigurationEditor;
import kaleidok.kaleidoscope.controls.KaleidoscopeControls;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchApplication;
import kaleidok.processing.SimplePAppletFactory;
import kaleidok.util.AssertionUtils;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class KaleidoscopeApp extends ProcessingSketchApplication<Kaleidoscope>
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeApp.class.getName());

  public static final String iconDir = "icons/";

  private Scene scene;

  private KaleidoscopeControls controls;

  private Stage configurationWindow;

  private KaleidoscopeConfigurationEditor configurationEditor;


  @Override
  public void init() throws Exception
  {
    super.init();
  }


  private Stage getConfigurationWindow()
  {
    if (configurationWindow == null)
    {
      configurationWindow = new Stage();
      configurationWindow.setTitle("Kaleidoscope configuration");
      configurationWindow.setScene(new Scene(getConfigurationEditor()));

      // TODO: Find a more suitable default position
      Rectangle2D screenBounds = Screen.getScreens().get(1).getBounds();
      configurationWindow.setX(screenBounds.getMinX());
      configurationWindow.setY(screenBounds.getMinY());
    }
    return configurationWindow;
  }


  private KaleidoscopeConfigurationEditor getConfigurationEditor()
  {
    if (configurationEditor == null)
    {
      configurationEditor = new KaleidoscopeConfigurationEditor(getSketch());
      configurationEditor.init();
    }
    return configurationEditor;
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


  private List<Image> makeIcons()
  {
    String iconPathname = iconDir + "start.png";
    URL iconUrl = getClass().getClassLoader().getResource(iconPathname);
    Exception ex;
    if (iconUrl != null) try {
      return Icons.makeIcons(iconUrl);
    } catch (Exception ex1) {
      ex = ex1;
    } else {
      ex = new FileNotFoundException(iconPathname);
    }
    logger.log(Level.WARNING, "Couldn't load application icon", ex);
    return Collections.emptyList();
  }


  @Override
  public void start( Stage stage ) throws Exception
  {
    stage.setTitle("Kaleidoscope Controls");
    stage.getIcons().addAll(makeIcons());
    stage.setOnCloseRequest((e) -> myStop());
    super.start(stage);
  }


  @SuppressWarnings("ProhibitedExceptionDeclared")
  @Override
  protected void show( Stage stage ) throws Exception
  {
    /*if (Double.isNaN(stage.getX()) || Double.isNaN(stage.getY()))
      placeAroundSketch(stage, 2, (Side[]) null);*/

    super.show(stage);
    getConfigurationWindow().show();
  }


  @Override
  public void stop() throws Exception
  {
    myStop();
    super.stop();
  }


  private void myStop()
  {
    if (configurationWindow != null)
      configurationWindow.close();
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
