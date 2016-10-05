package kaleidok.kaleidoscope.controls;

import javafx.beans.property.ReadOnlyProperty;
import javafx.geometry.Insets;
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


  private static final String
    START_RECORDING = "Start recording",
    STOP_RECORDING = "Stop recording";


  public ToggleButton getRecordingButton()
  {
    if (recordingButton == null)
    {
      final Image
        startIcon = loadIcon(iconDir + "start.png"),
        stopIcon = loadIcon(iconDir + "stop.png");
      ImageView buttonGraphics =
        (startIcon != null && stopIcon != null) ?
          new ImageView(startIcon) :
          null;

      // TODO: Make button "round" again
      recordingButton = new ToggleButton("Record", buttonGraphics);
      recordingButton.setTooltip(new Tooltip(START_RECORDING));
      if (buttonGraphics != null)
      {
        recordingButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        recordingButton.selectedProperty().addListener(
          (obs, oldValue, newValue) ->
            ((ImageView) ((Labeled) ((ReadOnlyProperty<?>) obs).getBean())
              .getGraphic()).setImage(newValue ? stopIcon : startIcon));
      }
      else
      {
        recordingButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
      }

      //recordingButton.paintUI = false;
      //recordingButton.setBorderPainted(false);

      recordingButton.selectedProperty().addListener(
        (obs, oldValue, newValue) ->
          ((Control) ((ReadOnlyProperty<?>) obs).getBean()).getTooltip()
            .setText(newValue ? STOP_RECORDING : START_RECORDING));
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
      Image icon = loadIcon(iconDir + "preferences.png");
      configurationWindowButton = new ToggleButton(
        "Configuration window", (icon != null) ? new ImageView(icon) : null);
      if (icon != null)
      {
        configurationWindowButton.setContentDisplay(
          ContentDisplay.GRAPHIC_ONLY);
        configurationWindowButton.setPadding(Insets.EMPTY);
      }
      configurationWindowButton.setTooltip(
        new Tooltip("Toggle configuration window"));

      configurationWindowButton.setOnAction((ev) -> {
          context.setShowConfigurationEditor(
            ((Toggle) ev.getSource()).isSelected());
          ev.consume();
        });
    }
    return configurationWindowButton;
  }
}
