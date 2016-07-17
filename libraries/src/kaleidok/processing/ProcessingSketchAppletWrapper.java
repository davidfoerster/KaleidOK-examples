package kaleidok.processing;

import kaleidok.swing.FullscreenAction.Mode;
import kaleidok.swing.FullscreenRootPane;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;
import processing.core.PConstants;

import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import static javax.swing.KeyStroke.getKeyStroke;


public abstract class ProcessingSketchAppletWrapper<T extends PApplet> extends JApplet
{
  private T sketch = null;


  private static final KeyStroke fullscreenKeystroke =
    (PApplet.platform == PConstants.MACOSX) ?
      getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK) :
      getKeyStroke(KeyEvent.VK_F11, 0);


  @Override
  public void init()
  {
    fudgeAppletViewer();

    T sketch;
    try {
      sketch = getSketchThrows();
    } catch (InvocationTargetException ex) {
      throw new Error(ex.getCause());
    }
    add(sketch, BorderLayout.CENTER);

    initWindowBounds();
    initComponents();
  }


  protected void initWindowBounds()
  {
    FullscreenRootPane rootPane = getRootPane();

    int screenIndex = DefaultValueParser.parseInt(this, "screen", -1);
    boolean fullscreen =
      DefaultValueParser.parseBoolean(this, "fullscreen", false);
    if (screenIndex >= 0 || fullscreen) {
      rootPane.moveToScreen(screenIndex, fullscreen);
    }

    Window w = rootPane.getTopLevelWindow();
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
    rootPane.registerKeyAction(Mode.TOGGLE, fullscreenKeystroke);
    return rootPane;
  }


  @Override
  public FullscreenRootPane getRootPane()
  {
    return (FullscreenRootPane) super.getRootPane();
  }


  private void fudgeAppletViewer()
  {
    for (Container current = this.getParent();
      current != null;
      current = current.getParent())
    {
      if (current instanceof Frame) {
        FudgedAppletViewerFactory.fudgeAppletViewer((Frame) current);
        break;
      }
    }
  }


  public T getSketch()
  {
    if (sketch != null)
      return sketch;

    throw new IllegalStateException(this + " hasn't been initialized");
  }


  protected T getSketchThrows() throws InvocationTargetException
  {
    if (sketch == null) {
      sketch = getSketchFactory().createInstance(this);
      sketch.init();
      sketch.addKeyListener(getRootPane().createKeyListener(
        Mode.TOGGLE, fullscreenKeystroke));
    }
    return sketch;
  }


  protected abstract PAppletFactory<T> getSketchFactory();


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
