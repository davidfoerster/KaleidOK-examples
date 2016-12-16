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
  implements PreferenceBean
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeApp.class.getName());

  public static final String iconDir = "icons/";

  private final ObjectProperty<Stage> primaryStage =
    new SimpleObjectProperty<>(this, "primary stage");

  private Scene scene;

  private KaleidoscopeControls controls;

  private final AspectedStringProperty messageFieldText;

  {
    messageFieldText = new AspectedStringProperty(this, "text");
    messageFieldText.addAspect(HiddenAspectTag.getInstance());
    messageFieldText.addAspect(PropertyPreferencesAdapterTag.getInstance());
  }


  private final ObjectProperty<Stage> configurationWindow =
    new SimpleObjectProperty<>(this, "configuration window");

  private KaleidoscopeConfigurationEditor configurationEditor;

  @SuppressWarnings("StaticFieldReferencedViaSubclass")
  private final GeometryPreferences configurationWindowPreferences =
    new GeometryPreferences(this, KaleidoscopeConfigurationEditor.class, true,
      GeometryPreferences.ALL);


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


  private synchronized Stage getConfigurationWindow()
  {
    if (configurationWindow.get() == null)
    {
      KaleidoscopeConfigurationEditor ce = getConfigurationEditor();
      Stage cw = new Stage();
      cw.setTitle(getSketch().getName() + " configuration");
      cw.setWidth(ce.getPrefWidth() + 20);
      cw.setScene(new Scene(ce));
      configurationWindow.set(cw);
      cw.showingProperty().addListener(
        ( obs, oldValue, newValue ) ->
          getControls().getConfigurationWindowButton().setSelected(newValue));
      Platform.runLater(() ->
        configurationWindowPreferences.applyGeometryAndBind(configurationWindow.get()));
    }
    return configurationWindow.get();
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
      setShowConfigurationEditor(true); // TODO: Move to background thread?
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
