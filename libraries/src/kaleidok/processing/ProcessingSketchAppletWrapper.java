package kaleidok.processing;

import kaleidok.swing.FullscreenAction;
import kaleidok.swing.FullscreenRootPane;
import processing.core.PApplet;

import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

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
      sketch.addKeyListener(new SketchKeyListener());
      sketchFactory = null;
    }
    return sketch;
  }


  private class SketchKeyListener extends KeyAdapter
  {
    @Override
    public void keyPressed( KeyEvent ev )
    {
      if (fullscreenKeystroke.getKeyEventType() == KeyEvent.KEY_PRESSED &&
        hasKeyStroke(ev, fullscreenKeystroke))
      {
        getRootPane().toggleFullscreen();
        ev.consume();
      }
    }
  }


  private static boolean hasKeyStroke( KeyEvent ev, KeyStroke stroke )
  {
    return
      (ev.getKeyCode() == stroke.getKeyCode()) &&
      hasAllModifiers(ev, stroke);
  }

  private static boolean hasAllModifiers( KeyEvent ev, KeyStroke stroke )
  {
    return
      ((ev.getModifiers() | ev.getModifiersEx()) & stroke.getModifiers())
        == stroke.getModifiers();
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


  static {
    String strTmpDir = System.getenv("TMPDIR");
    if (strTmpDir == null || strTmpDir.isEmpty())
      strTmpDir = System.getenv("TMP");
    if (strTmpDir != null && !strTmpDir.isEmpty()) {
      File fTmpDir = new File(strTmpDir);
      if (fTmpDir.isDirectory() && fTmpDir.canWrite())
        System.setProperty("java.io.tmpdir", strTmpDir);
    }
  }

}
