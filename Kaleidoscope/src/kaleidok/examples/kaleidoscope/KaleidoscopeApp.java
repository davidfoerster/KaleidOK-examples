package kaleidok.examples.kaleidoscope;

import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.util.DefaultValueParser;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;


public class KaleidoscopeApp extends ProcessingSketchAppletWrapper<Kaleidoscope>
{
  private JTextField keywordField = null, messageField = null;

  @Override
  public void init()
  {
    sketchFactory = new PAppletFactory<Kaleidoscope>()
      {
        @Override
        public Kaleidoscope createInstance( JApplet parent ) throws InstantiationException
        {
          return new Kaleidoscope(parent);
        }
      };

    super.init();
    initWindowPosition();
  }

  private void initWindowPosition()
  {
    String paramBase = getClass().getPackage().getName() + '.';
    int screenIndex = DefaultValueParser.parseInt(
      getParameter(paramBase + "screen"), -1);
    boolean fullscreen = DefaultValueParser.parseBoolean(
      getParameter(paramBase + "fullscreen"), false);
    if (screenIndex >= 0 || fullscreen)
      getRootPane().moveToScreen(screenIndex, fullscreen);
  }

  @Override
  protected void initComponents()
  {
    JTextField keywordField = getKeywordField(),
      messageField = getMessageField();

    JPanel textFieldPanels = new JPanel();
    textFieldPanels.setLayout(new BoxLayout(textFieldPanels, BoxLayout.PAGE_AXIS));
    textFieldPanels.add(messageField);
    textFieldPanels.add(keywordField);
    add(textFieldPanels, BorderLayout.SOUTH);

    messageField.requestFocusInWindow();
  }

  private JTextField getKeywordField()
  {
    if (keywordField == null) {
      keywordField = new JTextField();
    }
    return keywordField;
  }

  private JTextField getMessageField()
  {
    if (messageField == null) {
      messageField = new JTextField(getParameter(
        this.getClass().getPackage().getName() + ".text"));
      messageField.setAction(new AbstractAction()
      {
        @Override
        public void actionPerformed( ActionEvent ev )
        {
          try {
            // TODO: Don't do this in the event handler thread
            getSketch().getChromasthetiator().issueQuery(((JTextField) ev.getSource()).getText());
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    return messageField;
  }

}
