package kaleidok.kaleidoscope;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kaleidok.exaleads.chromatik.PropertyChromatikQuery;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.HiddenAspectTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.stage.GeometryPreferences;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class KaleidoscopeApp extends ProcessingSketchApplication<Kaleidoscope>
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeApp.class.getName());

  public static final String iconDir = "icons/";

  @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
  private Stage primaryStage;

  private Scene scene;

  private KaleidoscopeControls controls;

  private final AspectedStringProperty messageFieldText;

  {
    messageFieldText = new AspectedStringProperty(this, "text");
    messageFieldText.addAspect(HiddenAspectTag.getInstance());
    messageFieldText.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  private Stage configurationWindow;

  private KaleidoscopeConfigurationEditor configurationEditor;

  @SuppressWarnings("StaticFieldReferencedViaSubclass")
  private final GeometryPreferences configurationWindowPreferences =
    new GeometryPreferences(this, KaleidoscopeConfigurationEditor.class, true,
      GeometryPreferences.ALL);


  @Override
  public void init() throws Exception
  {
    super.init();
  }


  private synchronized Stage getConfigurationWindow()
  {
    if (configurationWindow == null)
    {
      KaleidoscopeConfigurationEditor ce = getConfigurationEditor();
      final Stage cw = configurationWindow = new Stage();
      cw.setTitle(getSketch().getName() + " configuration");
      cw.initOwner(primaryStage);
      cw.setWidth(ce.getPrefWidth() + 20);
      cw.setScene(new Scene(ce));
      cw.showingProperty().addListener(
        ( obs, oldValue, newValue ) ->
          getControls().getConfigurationWindowButton().setSelected(newValue));
      Platform.runLater(() ->
        configurationWindowPreferences.applyGeometryAndBind(cw));
    }
    return configurationWindow;
  }


  private KaleidoscopeConfigurationEditor getConfigurationEditor()
  {
    if (configurationEditor == null)
    {
      configurationEditor = new KaleidoscopeConfigurationEditor();

      Kaleidoscope sketch = getSketch();
      configurationEditor.addBean(sketch);

      configurationEditor.getSortOrder().add(
        configurationEditor.getColumns().get(0));
    }
    return configurationEditor;
  }


  public synchronized void setShowConfigurationEditor( boolean show )
  {
    if (show)
    {
       Stage cw = getConfigurationWindow();
       if (cw.getOwner() == null)
         cw.initOwner(primaryStage);
       cw.show();
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
    logger.log(Level.WARNING, "Couldn''t load application icon", ex);
    return Collections.emptyList();
  }


  @Override
  public void start( Stage stage ) throws Exception
  {
    stage.setTitle("Kaleidoscope Controls");
    stage.getIcons().addAll(makeIcons());
    stage.setOnCloseRequest((e) -> Platform.runLater(this::stopChildStages));
    super.start(stage);
    primaryStage = stage;

    if (configurationWindowPreferences.isShowing(false))
      getConfigurationEditor();
  }


  @SuppressWarnings("ProhibitedExceptionDeclared")
  @Override
  protected void show( Stage stage ) throws Exception
  {
    /*if (Double.isNaN(stage.getX()) || Double.isNaN(stage.getY()))
      placeAroundSketch(stage, 2, (Side[]) null);*/

    super.show(stage);

    if (configurationWindowPreferences.isShowing(false))
      setShowConfigurationEditor(true);
  }


  @Override
  public void stop() throws Exception
  {
    stopChildStages();
    super.stop();
  }


  private boolean childStagesStopped = false;

  private synchronized void stopChildStages()
  {
    if (childStagesStopped)
      return;
    childStagesStopped = true;

    //noinspection ConstantConditions
    configurationWindowPreferences.show.unbind();
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
      controls = new KaleidoscopeControls();

      TextField messageField = controls.getMessageField();
      messageField.setText(messageFieldText.get());
      messageFieldText.bind(messageField.textProperty());
      messageField.setOnAction((ev) -> {
          getSketch().getChromasthetiationService()
            .submit(((TextInputControl) ev.getSource()).getText());
          ev.consume();
        });

      PropertyChromatikQuery chromatikQuery = (PropertyChromatikQuery)
        getSketch().getChromasthetiationService().getChromasthetiator()
          .getChromatikQuery();
      controls.getKeywordField().textProperty().bindBidirectional(
        chromatikQuery.keywordProperty());

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


  @Override
  public Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    Stream<Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>> s = Stream.of(
      Stream.of(
        messageFieldText.getAspect(
          PropertyPreferencesAdapterTag.getInstance())),
      configurationWindowPreferences.getPreferenceAdapters(),
      super.getPreferenceAdapters());

    return s.flatMap(Function.identity());
  }
}
