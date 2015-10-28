package kaleidok.examples.kaleidoscope;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;


public class KaleidoscopeControls extends JFrame
{
  private static final String iconDir = "icons/";

  private final KaleidoscopeApp context;

  private JToggleButton recordingButton = null;

  private JPanel textFieldPanel = null;

  private JTextField keywordField = null, messageField = null;


  public KaleidoscopeControls( KaleidoscopeApp context )
  {
    super("Kaleidoscope Controls");
    this.context = context;

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    initComponents();
    pack();
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
      action.putValue(Action.SHORT_DESCRIPTION,
        "Start and stop recording");
      recordingButton = new JToggleButton(action);

      ClassLoader cl = this.getClass().getClassLoader();
      URL startIconUrl = cl.getResource(iconDir + "start.png"),
        stopIconUrl = cl.getResource(iconDir + "stop.png");
      assert startIconUrl != null && stopIconUrl != null;
      recordingButton.setText(null);
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
      messageField = new JTextField(context.getParameter(
        this.getClass().getPackage().getName() + ".text"));
      Dimension size = messageField.getPreferredSize();
      size.width = 250;
      messageField.setPreferredSize(size);

      messageField.setAction(new AbstractAction()
      {
        @Override
        public void actionPerformed( ActionEvent ev )
        {
          context.getSketch().getChromasthetiationService()
            .submit(messageField.getText());
        }
      });

      messageField.setToolTipText(
        "Write something emotional to analyze and press ENTER to submit");
    }
    return messageField;
  }
}
