package kaleidok.processing;

import javafx.scene.Scene;
import javafx.stage.Stage;
import kaleidok.javafx.PropertyLoaderApplication;
import kaleidok.util.LoggingUtils;
import processing.core.PApplet;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public abstract class ProcessingSketchApplication<T extends PApplet>
  extends PropertyLoaderApplication
{
  private T sketch = null;

  private Stage stage = null;

  protected final Preferences preferences =
    Preferences.userNodeForPackage(this.getClass());

  protected final String PREF_GEOMETRY =
    getClass().getSimpleName() + ".geometry.";


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
    getSketch().start();
    parseAndSetConfig(stage);
    stage.setScene(getScene());
    stage.show();
    this.stage = stage;
  }


  private void parseAndSetConfig( Stage stage )
  {
    double left = preferences.getDouble(PREF_GEOMETRY + "left", Double.NaN),
      top = preferences.getDouble(PREF_GEOMETRY + "top", Double.NaN);
    if (Double.isFinite(left) && Double.isFinite(top))
    {
      stage.setX(left);
      stage.setY(top);
    }
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void stop()
  {
    savePreferences();
    sketch.exit();
  }


  protected final void savePreferences()
  {
    doSavePreferences();
    try
    {
      preferences.flush();
    }
    catch (BackingStoreException ex)
    {
      LoggingUtils.logThrown(Logger.getLogger(getClass().getName()),
        Level.SEVERE, "Couldn't flush preference store: {0}", ex,
        preferences);
    }
  }


  protected void doSavePreferences()
  {
    Stage stage = this.stage;
    if (stage != null && !stage.isFullScreen())
    {
      String prefPrefix = PREF_GEOMETRY;
      Preferences preferences = this.preferences;
      preferences.putDouble(prefPrefix + "left", stage.getX());
      preferences.putDouble(prefPrefix + "top", stage.getY());
      preferences.putDouble(prefPrefix + "width", stage.getWidth());
      preferences.putDouble(prefPrefix + "height", stage.getHeight());
    }
  }
}
