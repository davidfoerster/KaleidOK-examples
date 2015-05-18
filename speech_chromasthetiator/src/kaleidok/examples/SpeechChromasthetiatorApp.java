package kaleidok.examples;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class SpeechChromasthetiatorApp extends JApplet
{
  private SpeechChromasthetiatorSketch sketch;

  private JTextField keywordField;

  public void init()
  {
    initComponents();
  }

  private void initComponents()
  {
    SpeechChromasthetiatorSketch sketch = getSketch();
    JTextField keywordField = getKeywordField();

    setSize(800, 200 + (int) keywordField.getPreferredSize().getHeight());

    add(sketch, BorderLayout.CENTER);
    add(keywordField, BorderLayout.SOUTH);

    keywordField.requestFocusInWindow();
  }

  private SpeechChromasthetiatorSketch getSketch()
  {
    if (sketch == null) {
      sketch = new SpeechChromasthetiatorSketch();
      sketch.keywordsDoc = getKeywordField().getDocument();
      sketch.init();
    }
    return sketch;
  }

  private JTextField getKeywordField()
  {
    if (keywordField == null) {
      keywordField = new JTextField();
      keywordField.setAction(new AbstractAction(){
        @Override
        public void actionPerformed( ActionEvent e )
        {
          sketch.requestFocusInWindow();
        }
      });
    }
    return keywordField;
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
