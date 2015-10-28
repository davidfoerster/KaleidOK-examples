package kaleidok.examples.kaleidoscope;

import kaleidok.processing.AppletLauncher;
import kaleidok.processing.FudgedAppletViewerFactory;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.swing.FullscreenEventListener;
import kaleidok.util.AssertionUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;


public class KaleidoscopeApp extends ProcessingSketchAppletWrapper<Kaleidoscope>
  implements FullscreenEventListener
{
  private JPanel textFieldPanel = null;

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
  }


  @Override
  protected void initComponents()
  {
    add(getTextFieldPanel(), BorderLayout.SOUTH);
    getRootPane().addFullscreenEventListener(this);
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
          getSketch().getChromasthetiationService()
            .submit(messageField.getText());
        }
      });
    }
    return messageField;
  }


  @Override
  public void handleFullscreenStateChange( GraphicsDevice dev, Window w,
    boolean fullscreen )
  {
    getTextFieldPanel().setVisible(!fullscreen);
    if (fullscreen) {
      getSketch().requestFocusInWindow();
    }
  }


  public static void main( String... args ) throws IOException
  {
    AssertionUtils.enableAssertionsOnDebugging();
    new AppletLauncher(new FudgedAppletViewerFactory())
      .launch(KaleidoscopeApp.class, args);
  }
}
