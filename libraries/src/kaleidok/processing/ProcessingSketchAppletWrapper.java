package kaleidok.processing;

import kaleidok.swing.FullscreenAction;
import kaleidok.swing.FullscreenRootPane;

import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class ProcessingSketchAppletWrapper<T extends ExtPApplet> extends JApplet
{
  public PAppletFactory<T> sketchFactory = null;

  private T sketch = null;

  private static final KeyStroke fullscreenKeystroke =
    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);

  @Override
  public void init()
  {
    fudgeAppletViewer();

    T sketch;
    try {
      sketch = getSketchThrows();
    } catch (InstantiationException e) {
      throw new Error(e);
    }
    add(sketch, BorderLayout.CENTER);

    initComponents();
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
        fudgeAppletViewer((Frame) current);
        break;
      }
    }
  }

  private static void fudgeAppletViewer( Frame frame )
  {
    synchronized (frame.getTreeLock()) {
      frame.setMenuBar(null);
      int len = frame.getComponentCount();
      for (int i = 0; i < len; i++) {
        if (frame.getComponent(i) instanceof Label) {
          frame.remove(i);
          break;
        }
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
      sketch.addKeyListener(new KeyAdapter()
        {
          @Override
          public void keyPressed( KeyEvent e )
          {
            if (e.getKeyCode() == fullscreenKeystroke.getKeyCode() &&
              e.getModifiers() == fullscreenKeystroke.getModifiers())
            {
              getRootPane().toggleFullscreen();
              e.consume();
            }
          }
        });
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

}
