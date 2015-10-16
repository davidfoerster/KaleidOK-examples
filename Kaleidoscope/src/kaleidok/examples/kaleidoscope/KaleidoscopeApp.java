package kaleidok.examples.kaleidoscope;

import kaleidok.processing.AppletLauncher;
import kaleidok.processing.FudgedAppletViewerFactory;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.swing.FullscreenEventListener;
import kaleidok.util.AssertionUtils;
import kaleidok.util.DefaultValueParser;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;


public class KaleidoscopeApp extends ProcessingSketchAppletWrapper<Kaleidoscope>
  implements FullscreenEventListener
{
  private JPanel textFieldPanel = null;

  private JTextField keywordField = null, messageField = null;

  private static final String paramBase =
    KaleidoscopeApp.class.getPackage().getName() + '.';


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

    initWindowPosition();
    super.init();
  }

  private void initWindowPosition()
  {
    int screenIndex = DefaultValueParser.parseInt(
      getParameter("screen"), -1);
    boolean fullscreen = DefaultValueParser.parseBoolean(
      getParameter("fullscreen"), false);
    if (screenIndex >= 0 || fullscreen) {
      getRootPane().moveToScreen(screenIndex, fullscreen);
    }
    handleFullscreenStateChange(null, null, fullscreen);
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
      messageField = new JTextField(getParameter(paramBase + "text"));
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
