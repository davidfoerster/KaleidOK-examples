package kaleidok.kaleidoscope;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import kaleidok.exaleads.chromatik.PropertyChromatikQuery;
import kaleidok.javafx.beans.property.AspectedStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.HiddenAspectTag;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.javafx.stage.GeometryPreferences;
import kaleidok.javafx.stage.Icons;
import kaleidok.javafx.stage.StageOwnerInitializer;
import kaleidok.kaleidoscope.controls.ConfigurationEditorScene;
import kaleidok.kaleidoscope.controls.ConfigurationEditorTreeTable;
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
  implements PreferenceBean
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeApp.class.getName());

  public static final String iconDir = "icons/";

  private final ObjectProperty<Stage> primaryStage =
    new SimpleObjectProperty<>(this, "primary stage");

  private Scene primaryScene;

  private KaleidoscopeControls controls;

  private final AspectedStringProperty messageFieldText;

  {
    messageFieldText = new AspectedStringProperty(this, "text");
    messageFieldText.addAspect(HiddenAspectTag.getInstance());
    messageFieldText.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  private final ObjectProperty<Stage> configurationWindow =
    new SimpleObjectProperty<>(this, "configuration window");

  @SuppressWarnings("StaticFieldReferencedViaSubclass")
  private final GeometryPreferences configurationWindowPreferences =
    new GeometryPreferences(this, ConfigurationEditorScene.class, true,
      GeometryPreferences.ALL);

  private ConfigurationEditorScene configurationEditorScene;


  public KaleidoscopeApp()
  {
    StageOwnerInitializer.apply(primaryStage, configurationWindow);
  }


  @Override
  public void init() throws Exception
  {
    super.init();

    getPreferenceAdapters()
      .forEach(ReadOnlyPropertyPreferencesAdapter::loadIfWritable);
  }


  private synchronized ConfigurationEditorScene getConfigurationEditorScene()
  {
    if (configurationEditorScene == null)
    {
      configurationEditorScene = new ConfigurationEditorScene();
      ConfigurationEditorTreeTable cett =
        configurationEditorScene.configurationEditor;
      cett.addBean(getSketch());
      cett.getSortOrder().add(
        cett.getColumns().get(0));
    }
    return configurationEditorScene;
  }


  private synchronized Stage getConfigurationWindow()
  {
    if (configurationWindow.get() == null)
    {
      Stage cw = new Stage();
      cw.setTitle(getSketch().getName() + " Preferences");
      getConfigurationEditorScene().start(cw);
      configurationWindow.set(cw);
      cw.showingProperty().addListener(
        ( obs, oldValue, newValue ) ->
          getControls().getConfigurationWindowButton().setSelected(newValue));
      Platform.runLater(() ->
        configurationWindowPreferences.applyGeometryAndBind(configurationWindow.get()));
    }
    return configurationWindow.get();
  }


  public synchronized void setShowConfigurationEditor( boolean show )
  {
    if (show)
    {
       getConfigurationWindow().show();
    }
    else if (configurationWindow.get() != null)
    {
      configurationWindow.get().hide();
    }
  }


  @Override
  protected synchronized Scene getScene()
  {
    if (primaryScene == null)
    {
      primaryScene = new Scene(getControls());
    }
    return primaryScene;
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
  public void start( Stage stage )
  {
    primaryStage.set(stage);
    stage.setTitle("Kaleidoscope Controls");
    stage.getIcons().addAll(makeIcons());
    stage.setOnCloseRequest((e) -> Platform.runLater(this::stopChildStages));
    super.start(stage);
  }


  @Override
  protected void show( Stage stage )
  {
    /*if (Double.isNaN(stage.getX()) || Double.isNaN(stage.getY()))
      placeAroundSketch(stage, 2, (Side[]) null);*/

    super.show(stage);

    if (configurationWindowPreferences.isShowing(false))
    {
      executorService.submit(() -> {
        // Construct the scene in a background thread
        getConfigurationEditorScene();
        // Construct and show the stage on the JavaFX application thread
        Platform.runLater(() -> getConfigurationWindow().show());
      });
    }
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
    if (configurationWindow.get() != null)
      configurationWindow.get().close();
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
  public String getName()
  {
    //noinspection SpellCheckingInspection
    return "KaleidOK";
  }


  @Override
  public Object getParent()
  {
    return null;
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
