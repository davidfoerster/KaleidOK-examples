package kaleidok.examples.kaleidoscope;

import kaleidok.util.DebugManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class KaleidoscopeApp extends JApplet
{
  private Kaleidoscope sketch;

  private JTextField keywordField, messageField;

  public void init()
  {
    DebugManager.fromApplet(this);
    initComponents();
  }

  private void initComponents()
  {
    Kaleidoscope sketch = getSketch();
    JTextField keywordField = getKeywordField(),
      messageField = getMessageField();
    double preferredHeight =
      keywordField.getPreferredSize().getHeight() +
      messageField.getPreferredSize().getHeight();

    setSize(1000, 1000 + (int) preferredHeight);

    add(sketch, BorderLayout.CENTER);
    JPanel textFieldPanels = new JPanel();
    textFieldPanels.setLayout(new BoxLayout(textFieldPanels, BoxLayout.PAGE_AXIS));
    textFieldPanels.add(messageField);
    textFieldPanels.add(keywordField);
    add(textFieldPanels, BorderLayout.SOUTH);

    messageField.requestFocusInWindow();
  }

  private Kaleidoscope getSketch()
  {
    if (sketch == null) {
      sketch = new Kaleidoscope(getParameter("audio.input"));
      sketch.chromasthetiator.keywordsDoc = getKeywordField().getDocument();
      sketch.init();
    }
    return sketch;
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
      messageField = new JTextField("happiness");
      messageField.setAction(new AbstractAction()
      {
        @Override
        public void actionPerformed( ActionEvent ev )
        {
          try {
            sketch.chromasthetiator.issueQuery(((JTextField) ev.getSource()).getText());
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      });
    }
    return messageField;
  }

  @Override
  public void start()
  {
    sketch.start();
  }

  @Override
  public void stop()
  {
    if (sketch != null)
      sketch.stop();
  }
}
