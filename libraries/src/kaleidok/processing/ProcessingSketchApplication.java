package kaleidok.processing;

import javafx.scene.Scene;
import javafx.stage.Stage;
import kaleidok.javafx.PropertyLoaderApplication;
import processing.core.PApplet;
import processing.core.PConstants;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import static javax.swing.KeyStroke.getKeyStroke;


public abstract class ProcessingSketchApplication<T extends PApplet>
  extends PropertyLoaderApplication
{
  private T sketch = null;


  // TODO: Move to wherever it's needed, or remove it entirely
  private static final KeyStroke fullscreenKeystroke =
    (PApplet.platform == PConstants.MACOSX) ?
      getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK) :
      getKeyStroke(KeyEvent.VK_F11, 0);


  @Override
  @OverridingMethodsMustInvokeSuper
  public void init() throws Exception
  {
    super.init();
    initSketch();
  }


  /*
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
  */


  protected abstract Scene getScene();


  public T getSketch()
  {
    if (sketch != null)
      return sketch;

    throw new IllegalStateException(this + " hasn't been initialized");
  }


  protected void initSketch()
  {
    if (sketch != null)
    {
      throw new AssertionError(new IllegalStateException(
        sketch.getClass().getCanonicalName() + " is already initialized"));
    }

    try
    {
      sketch =
        getSketchFactory().createInstance(this, getParameters().getRaw());
    }
    catch (InvocationTargetException ex)
    {
      throw new Error(ex.getCause());
    }
  }


  protected abstract PAppletFactory<T> getSketchFactory();


  @Override
  @OverridingMethodsMustInvokeSuper
  public void start( Stage stage )
  {
    stage.setScene(getScene());
    stage.show();

    sketch.start();
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void stop()
  {
    sketch.exit();
  }
}
