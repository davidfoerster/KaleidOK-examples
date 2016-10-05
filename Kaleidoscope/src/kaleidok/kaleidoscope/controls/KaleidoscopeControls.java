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

  private ToggleButton recordingButton;

  private VBox bottomPanel;

  private TextField keywordField, messageField;


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


  private ToggleButton getRecordingButton()
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
      recordingButton.setOnAction((ev) -> {
          boolean selected = ((Toggle) ev.getSource()).isSelected();
          context.getSketch().getSTT().setRecorderStatus(selected, false);
          buttonGraphics.setImage(selected ? stopIcon : startIcon);
          ev.consume();
        });
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
      bottomPanel = new VBox(getMessageField(), getKeywordField());
    }
    return bottomPanel;
  }


  private TextField getKeywordField()
  {
    if (keywordField == null)
    {
      keywordField = new TextField();
      keywordField.setPromptText("Keywords for image search");
    }
    return keywordField;
  }


  private TextField getMessageField()
  {
    if (messageField == null)
    {
      messageField =
        new TextField(context.getNamedParameters().get(
          this.getClass().getPackage().getName() + ".text"));
      messageField.setPrefWidth(250);
      messageField.setOnAction((ev) -> {
          context.getSketch().getChromasthetiationService()
            .submit(((TextInputControl) ev.getSource()).getText());
          ev.consume();
        });
      messageField.setPromptText(
        "Write something emotional to analyze and press ENTER to submit");
    }
    return messageField;
  }
}
