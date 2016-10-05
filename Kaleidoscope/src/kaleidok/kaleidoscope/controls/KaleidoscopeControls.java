package kaleidok.kaleidoscope.controls;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import kaleidok.kaleidoscope.KaleidoscopeApp;

import java.util.logging.Level;
import java.util.logging.Logger;

import static kaleidok.kaleidoscope.KaleidoscopeApp.iconDir;
import static kaleidok.util.logging.LoggingUtils.logThrown;


public class KaleidoscopeControls extends BorderPane
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeApp.class.getName());


  private final KaleidoscopeApp context;

  private ToggleButton recordingButton, configurationWindowButton;

  private VBox bottomPanel;

  private TextField keywordField, messageField;

  private ToolBar toolBar;


  public KaleidoscopeControls( KaleidoscopeApp context )
  {
    this.context = context;
    initComponents();
  }


  private void initComponents()
  {
    setCenter(getRecordingButton());
    setBottom(getBottomPanel());
  }


  public ToggleButton getRecordingButton()
  {
    if (recordingButton == null)
    {
      final Image
        startIcon = loadIcon(iconDir + "start.png"),
        stopIcon = loadIcon(iconDir + "stop.png");
      final ImageView buttonGraphics = new ImageView(startIcon);

      // TODO: Make button "round" again
      recordingButton = new ToggleButton("Record", buttonGraphics);
      recordingButton.setContentDisplay(
        (startIcon.isError() || stopIcon.isError()) ?
          ContentDisplay.TEXT_ONLY :
          ContentDisplay.GRAPHIC_ONLY);
      //recordingButton.paintUI = false;
      //recordingButton.setBorderPainted(false);
      recordingButton.selectedProperty().addListener(
        (obs, oldValue, newValue) ->
          buttonGraphics.setImage(newValue ? stopIcon : startIcon));
    }
    return recordingButton;
  }


  private Image loadIcon( String url )
  {
    Image img = new Image(url, false);
    if (img.isError())
    {
      if (this.getClass().desiredAssertionStatus())
      {
        throw new AssertionError(
          "Couldn't load icon: " + url, img.getException());
      }

      logThrown(logger, Level.WARNING,
        "Couldn't load icon: {0}",
        img.getException(), url);
      img = null;
    }
    return img;
  }


  private VBox getBottomPanel()
  {
    if (bottomPanel == null)
    {
      bottomPanel = new VBox(
        getMessageField(),
        getKeywordField(),
        getToolBar());
    }
    return bottomPanel;
  }


  public TextField getKeywordField()
  {
    if (keywordField == null)
    {
      keywordField = new TextField();
      keywordField.setPromptText("Keywords for image search");
    }
    return keywordField;
  }


  public TextField getMessageField()
  {
    if (messageField == null)
    {
      messageField =
        new TextField(context.getNamedParameters().get(
          this.getClass().getPackage().getName() + ".text"));
      messageField.setPrefWidth(250);
      messageField.setPromptText(
        "Write something emotional to analyze and press ENTER to submit");
    }
    return messageField;
  }


  public ToolBar getToolBar()
  {
    if (toolBar == null)
    {
      toolBar = new ToolBar(getConfigurationWindowButton());
    }
    return toolBar;
  }


  public ToggleButton getConfigurationWindowButton()
  {
    if (configurationWindowButton == null)
    {
      configurationWindowButton = new ToggleButton("Configuration window");
      configurationWindowButton.setOnAction((ev) -> {
          context.setShowConfigurationEditor(
            ((Toggle) ev.getSource()).isSelected());
          ev.consume();
        });
    }
    return configurationWindowButton;
  }
}
