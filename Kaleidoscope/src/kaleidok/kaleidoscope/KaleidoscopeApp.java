package kaleidok.kaleidoscope;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kaleidok.javafx.stage.Icons;
import kaleidok.kaleidoscope.controls.KaleidoscopeConfigurationEditor;
import kaleidok.kaleidoscope.controls.KaleidoscopeControls;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchApplication;
import kaleidok.processing.SimplePAppletFactory;
import kaleidok.util.AssertionUtils;

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


  private synchronized Stage getConfigurationWindow()
  {
    if (configurationWindow == null)
    {
      configurationWindow = new Stage();
      configurationWindow.setTitle("Kaleidoscope configuration");
      configurationWindow.setWidth(300);
      configurationWindow.setScene(new Scene(getConfigurationEditor()));

      // TODO: Find a more suitable default position
      Rectangle2D screenBounds = Screen.getScreens().get(1).getBounds();
      configurationWindow.setX(screenBounds.getMinX());
      configurationWindow.setY(screenBounds.getMinY());

      configurationWindow.showingProperty().addListener(
        ( obs, oldValue, newValue ) ->
          getControls().getConfigurationWindowButton().setSelected(newValue));
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


  public synchronized void setShowConfigurationEditor( boolean show )
  {
    if (show)
    {
      getConfigurationWindow().show();
    }
    else if (configurationWindow != null)
    {
      configurationWindow.hide();
    }
  }


  @Override
  protected synchronized Scene getScene()
  {
    if (scene == null)
    {
      scene = new Scene(getControls());
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


  private synchronized void myStop()
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
    {
      controls = new KaleidoscopeControls(this);

      controls.getMessageField().setOnAction((ev) -> {
          getSketch().getChromasthetiationService()
            .submit(((TextInputControl) ev.getSource()).getText());
          ev.consume();
        });

      controls.getRecordingButton().setOnAction((ev) -> {
          getSketch().getSTT().setRecorderStatus(
            ((Toggle) ev.getSource()).isSelected(), false);
          ev.consume();
        });

      controls.getConfigurationWindowButton().setOnAction((ev) -> {
          setShowConfigurationEditor(
            ((Toggle) ev.getSource()).isSelected());
          ev.consume();
        });
    }
    return controls;
  }


  public static void main( String... args )
  {
    AssertionUtils.enableAssertionsOnDebugging();
    launch(KaleidoscopeApp.class, args);
  }
}
