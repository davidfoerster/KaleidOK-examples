package kaleidok.processing;

import kaleidok.swing.FullscreenAction;
import kaleidok.swing.FullscreenRootPane;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.KeyEvent;

import static javax.swing.KeyStroke.getKeyStroke;


public class ProcessingSketchAppletWrapper<T extends ExtPApplet> extends JApplet
{
  public PAppletFactory<T> sketchFactory = null;

  private T sketch = null;


  private static final KeyStroke fullscreenKeystroke =
    (PApplet.platform == PApplet.MACOSX) ?
      getKeyStroke(KeyEvent.VK_F, KeyEvent.META_DOWN_MASK) :
      getKeyStroke(KeyEvent.VK_F11, 0);


  @Override
  public void init()
  {
    fudgeAppletViewer();

    T sketch;
    try {
      sketch = getSketchThrows();
    } catch (InstantiationException ex) {
      throw new Error(ex);
    }
    add(sketch, BorderLayout.CENTER);

    initWindowBounds();
    initComponents();
  }


  protected void initWindowBounds()
  {
    int screenIndex = DefaultValueParser.parseInt(this, "screen", -1);
    boolean fullscreen =
      DefaultValueParser.parseBoolean(this, "fullscreen", false);
    if (screenIndex >= 0 || fullscreen) {
      getRootPane().moveToScreen(screenIndex, fullscreen);
    }

    Window w = getRootPane().getTopLevelWindow();
    Rectangle screenBounds = w.getGraphicsConfiguration().getBounds();
    w.setLocation(
      DefaultValueParser.parseInt(this, "left", w.getX() - screenBounds.x) +
        screenBounds.x,
      DefaultValueParser.parseInt(this, "top", w.getY() - screenBounds.y) +
        screenBounds.y);
  }


  protected void initComponents() { }


  @Override
  protected JRootPane createRootPane()
  {
    FullscreenRootPane rootPane = new FullscreenRootPane();
    rootPane.registerKeyAction(FullscreenAction.Mode.TOGGLE, fullscreenKeystroke);
    return rootPane;
  }


  @Override
  public FullscreenRootPane getRootPane()
  {
    return (FullscreenRootPane) super.getRootPane();
  }


  private void fudgeAppletViewer()
  {
    Container current = this, parent;
    while ((parent = current.getParent()) != null) {
      current = parent;
      if (current instanceof Frame) {
        FudgedAppletViewerFactory.fudgeAppletViewer((Frame) current);
        break;
      }
    }
  }


  public T getSketch()
  {
    if (sketch == null) try {
      return getSketchThrows();
    } catch (InstantiationException ex) {
      ex.printStackTrace();
    }
    return sketch;
  }

  protected T getSketchThrows() throws InstantiationException
  {
    if (sketch == null) {
      sketch = sketchFactory.createInstance(this);
      sketch.init();
      sketch.addKeyListener(getRootPane().createKeyListener(
        FullscreenAction.Mode.TOGGLE, fullscreenKeystroke));
      sketchFactory = null;
    }
    return sketch;
  }


  @Override
  public void start()
  {
    sketch.start();
  }

  @Override
  public void stop()
  {
    sketch.stop();
  }

  @Override
  public void destroy()
  {
    sketch.destroy();
  }
}
