package kaleidok.kaleidoscope.controls;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import static kaleidok.kaleidoscope.KaleidoscopeApp.iconDir;
import static kaleidok.util.logging.LoggingUtils.logAssertion;


public class KaleidoscopeControls extends BorderPane
{
  static final Logger logger =
    Logger.getLogger(KaleidoscopeControls.class.getName());


  private ToggleButton recordingButton, configurationWindowButton;

  private VBox bottomPanel;

  private TextField keywordField, messageField;

  private ToolBar toolBar;


  public KaleidoscopeControls()
  {
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
      recordingButton = makeRoundToggleButton(
        "Record", loadIcon(iconDir + "start.png"),
        loadIcon(iconDir + "stop.png"));
      Tooltip tooltip = new Tooltip();
      tooltip.textProperty().bind(
        Bindings.when(recordingButton.selectedProperty())
          .then("Start recording").otherwise("Stop recording"));
      recordingButton.setTooltip(tooltip);
    }
    return recordingButton;
  }


  static Image loadIcon( String url )
  {
    return loadIcon(url, false);
  }

  static Image loadIcon( String url, boolean backgroundLoading )
  {
    Image img = new Image(url, backgroundLoading);
    if (img.isError())
    {
      logAssertion(KaleidoscopeControls.class, logger, Level.WARNING,
        "Couldn’t load icon: {0}", img.getException(), url);
    }
    return img;
  }


  private static ToggleButton makeRoundToggleButton( String text,
    Image unselectedImage, Image selectedImage )
  {
    ImageView buttonGraphics =
      (!unselectedImage.isError() && !selectedImage.isError()) ?
        new ImageView() :
        null;
    ToggleButton button = new ToggleButton(text, buttonGraphics);
    if (buttonGraphics != null)
    {
      button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      button.setBorder(Border.EMPTY);
      button.setPadding(Insets.EMPTY);
      BorderPane.setMargin(button, new Insets(10));

      @SuppressWarnings("OptionalGetWithoutIsPresent")
      double diameter =
        DoubleStream.of(
          unselectedImage.getWidth(), unselectedImage.getHeight(),
          selectedImage.getWidth(), selectedImage.getHeight())
          .max().getAsDouble();
      button.setShape(new Circle(diameter * 0.5));
      button.setPickOnBounds(false);
      buttonGraphics.imageProperty().bind(
        Bindings.when(button.selectedProperty())
          .then(selectedImage).otherwise(unselectedImage));
    }
    else
    {
      button.setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    return button;
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
      messageField = new TextField();
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
        "Configuration window", !icon.isError() ? new ImageView(icon) : null);
      if (!icon.isError())
      {
        configurationWindowButton.setContentDisplay(
          ContentDisplay.GRAPHIC_ONLY);
        configurationWindowButton.setPadding(new Insets(2.5));

        configurationWindowButton.getGraphic().effectProperty().bind(
          Bindings.when(configurationWindowButton.selectedProperty())
            .then(new ColorAdjust(0, 0, -0.25, 0))
            .otherwise((ColorAdjust) null));
      }
      configurationWindowButton.setTooltip(
        new Tooltip("Toggle configuration window"));
    }
    return configurationWindowButton;
  }
}
