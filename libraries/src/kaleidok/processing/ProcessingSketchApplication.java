package kaleidok.processing;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kaleidok.javafx.PropertyLoaderApplication;
import kaleidok.javafx.geometry.Rectangles;
import kaleidok.javafx.stage.Screens;
import kaleidok.util.LoggingUtils;
import processing.core.PApplet;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
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
  public void start( Stage stage ) throws Exception
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


  @SuppressWarnings("ConfusingElseBranch")
  public Side placeAroundSketch( Stage stage, double padding,
    Side... preferredSides )
    throws InterruptedException
  {
    PApplet sketch = getSketch();

    if (sketch.sketchFullScreen())
    {
      Map.Entry<Side, Screen> neighborScreenSide =
        Screens.getNeighborScreen(sketch.sketchDisplay() - 1, preferredSides);
      if (neighborScreenSide == null)
        return null;
      Rectangle2D neighborScreen = neighborScreenSide.getValue().getBounds();
      stage.setX(
        neighborScreen.getMinX() + (neighborScreen.getWidth() - stage.getWidth()) * 0.5);
      stage.setY(
        neighborScreen.getMinY() + (neighborScreen.getHeight() - stage.getHeight()) * 0.5);
      return neighborScreenSide.getKey();
    }
    else if (sketch instanceof ExtPApplet)
    {
      ExtPApplet extSketch = (ExtPApplet) sketch;

      extSketch.awaitShowSurface();
      Map.Entry<Side, Point2D> stageLocation =
        Screens.placeAround(stage.getWidth(), stage.getHeight(),
          Rectangles.from(extSketch.getWindowBounds(null)), padding,
          preferredSides);
      if (stageLocation == null)
        return null;
      stage.setX(stageLocation.getValue().getX());
      stage.setY(stageLocation.getValue().getY());
      return stageLocation.getKey();
    }
    else
    {
      throw new UnsupportedOperationException(
        "The sketch class " + sketch.getClass().getName() +
          " isn't derived from " + ExtPApplet.class.getName());
    }
  }
}
