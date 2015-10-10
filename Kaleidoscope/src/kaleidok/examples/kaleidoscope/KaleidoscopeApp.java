package kaleidok.examples.kaleidoscope;

import kaleidok.io.platform.PlatformPaths;
import kaleidok.processing.AppletLauncher;
import kaleidok.processing.FudgedAppletViewerFactory;
import kaleidok.processing.PAppletFactory;
import kaleidok.processing.ProcessingSketchAppletWrapper;
import kaleidok.swing.FullscreenEventListener;
import kaleidok.util.DefaultValueParser;
import org.apache.commons.io.output.TeeOutputStream;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


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
      getParameter(paramBase + "screen"), -1);
    boolean fullscreen = DefaultValueParser.parseBoolean(
      getParameter(paramBase + "fullscreen"), false);
    if (screenIndex >= 0 || fullscreen)
      getRootPane().moveToScreen(screenIndex, fullscreen);
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
  }


  public static void main( String... args ) throws IOException
  {
    Class<? extends JApplet> appletClass = KaleidoscopeApp.class;
    Properties properties = new Properties();
    if (args != null && args.length > 0 && args[0].equals("--params")) {
      String paramsFile = args[1];
      properties.load(
        paramsFile.equals("-") ?
          new InputStreamReader(System.in) :
          new FileReader(paramsFile));

      args = (args.length > 2) ? Arrays.copyOfRange(args, 2, args.length) : null;
    } else {
      properties.load(appletClass.getResourceAsStream(
        appletClass.getSimpleName() + ".properties"));
    }

    // TODO: Hack to append to log file
    Path logfilePath =
      PlatformPaths.INSTANCE.getDataDir(appletClass.getPackage().getName())
        .resolve("logfile.log");
    System.out.println(
      "Appending all subsequent program output to: " + logfilePath);
    System.out.flush();
    System.setOut(new PrintStream(new TeeOutputStream(
      System.out, Files.newOutputStream(logfilePath, WRITE, CREATE, APPEND))));
    System.out.format("Starting new instance of %s at %tc.%n",
      appletClass.getCanonicalName(), System.currentTimeMillis());

    new AppletLauncher(new FudgedAppletViewerFactory())
      .launch(appletClass, properties, args);
  }
}
