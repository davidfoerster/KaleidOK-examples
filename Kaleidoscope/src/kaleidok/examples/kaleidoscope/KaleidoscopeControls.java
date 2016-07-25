package kaleidok.examples.kaleidoscope;

import kaleidok.swing.JRoundToggleButton;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;

import static javax.swing.Action.SHORT_DESCRIPTION;


public class KaleidoscopeControls extends JPanel
{
  private static final String iconDir = "icons/";

  private final KaleidoscopeApp context;

  private JRoundToggleButton recordingButton = null;

  private JPanel textFieldPanel = null;

  private JTextField keywordField = null, messageField = null;


  public KaleidoscopeControls( KaleidoscopeApp context )
  {
    this.context = context;
    initComponents();
  }


  private void initComponents()
  {
    add(getRecordingButton(), BorderLayout.NORTH);
    add(getTextFieldPanel(), BorderLayout.SOUTH);
  }


  private JToggleButton getRecordingButton()
  {
    if (recordingButton == null) {
      AbstractAction action = new AbstractAction("Record") {
          @Override
          public void actionPerformed( ActionEvent ev )
          {
            context.getSketch().getSTT().setRecorderStatus(
              ((AbstractButton) ev.getSource()).isSelected(), false);
          }
        };
      action.putValue(SHORT_DESCRIPTION, "Start and stop recording");
      recordingButton = new JRoundToggleButton(action);
      recordingButton.paintUI = false;
      recordingButton.setText(null);
      recordingButton.setBorderPainted(false);

      ClassLoader cl = this.getClass().getClassLoader();
      URL startIconUrl = cl.getResource(iconDir + "start.png"),
        stopIconUrl = cl.getResource(iconDir + "stop.png");
      assert startIconUrl != null && stopIconUrl != null;
      recordingButton.setIcon(
        new ImageIcon(startIconUrl, "Start recording"));
      recordingButton.setSelectedIcon(
        new ImageIcon(stopIconUrl, "Stop recording"));
    }
    return recordingButton;
  }


  private JPanel getTextFieldPanel()
  {
    if (textFieldPanel == null) {
      textFieldPanel = new JPanel();
      textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.PAGE_AXIS));
      textFieldPanel.add(getMessageField());
      textFieldPanel.add(getKeywordField());
    }
    return textFieldPanel;
  }


  private JTextField getKeywordField()
  {
    if (keywordField == null) {
      keywordField = new JTextField();
      keywordField.setToolTipText("Keywords for image search");
    }
    return keywordField;
  }


  private JTextField getMessageField()
  {
    if (messageField == null) {
      messageField =
        new JTextField(context.getNamedParameters().get(
          this.getClass().getPackage().getName() + ".text"));
      Dimension size = messageField.getPreferredSize();
      size.width = 250;
      messageField.setPreferredSize(size);

      AbstractAction action = new AbstractAction("Submit")
      {
        @Override
        public void actionPerformed( ActionEvent ev )
        {
          context.getSketch().getChromasthetiationService()
            .submit(messageField.getText());
        }
      };
      action.putValue(SHORT_DESCRIPTION,
        "Submit text and keywords for chromasthetiation");
      messageField.setAction(action);

      messageField.setToolTipText(
        "Write something emotional to analyze and press ENTER to submit");
    }
    return messageField;
  }
}
