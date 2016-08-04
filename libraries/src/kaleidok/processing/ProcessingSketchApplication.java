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
import java.util.AbstractMap;
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
    show(stage);
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


  @SuppressWarnings("ProhibitedExceptionDeclared")
  protected void show( Stage stage ) throws Exception
  {
    stage.show();
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void stop() throws Exception
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


  public Side placeAroundSketch( Stage stage, double padding,
    Side... preferredSides )
    throws InterruptedException
  {
    PApplet sketch = getSketch();
    Map.Entry<Side, Point2D> placement;

    if (sketch.sketchFullScreen())
    {
      placement = placeAroundFullscreenSketch(padding, preferredSides);
    }
    else if (sketch instanceof ExtPApplet)
    {
      placement = placeAroundWindowedSketch(padding, preferredSides);
    }
    else
    {
      throw new UnsupportedOperationException(
        "The sketch class " + sketch.getClass().getName() +
          " isn't derived from " + ExtPApplet.class.getName());
    }

    if (placement == null)
      return null;

    Point2D l = placement.getValue();
    stage.setX(l.getX());
    stage.setY(l.getY());
    return placement.getKey();
  }


  private Map.Entry<Side, Point2D> placeAroundWindowedSketch( double padding,
    Side... preferredSides ) throws InterruptedException
  {
    ExtPApplet extSketch = (ExtPApplet) getSketch();
    Scene scene = getScene();
    extSketch.awaitShowSurface();
    return
      Screens.placeAround(scene.getWidth(), scene.getHeight(),
        Rectangles.from(extSketch.getWindowBounds(null)), padding,
        preferredSides);
  }


  private Map.Entry<Side, Point2D> placeAroundFullscreenSketch(
    @SuppressWarnings("UnusedParameters") double padding,
    Side... preferredSides )
  {
    Map.Entry<Side, Screen> neighborScreenSide =
      Screens.getNeighborScreen(getSketch().sketchDisplay() - 1, preferredSides);
    if (neighborScreenSide == null)
      return null;

    Scene scene = getScene();
    Rectangle2D neighborScreen = neighborScreenSide.getValue().getBounds();
    return new AbstractMap.SimpleEntry<>(
      neighborScreenSide.getKey(),
      new Point2D(
        neighborScreen.getMinX() +
          (neighborScreen.getWidth() - scene.getWidth()) * 0.5,
        neighborScreen.getMinY() +
          (neighborScreen.getHeight() - scene.getHeight()) * 0.5));
  }
}
