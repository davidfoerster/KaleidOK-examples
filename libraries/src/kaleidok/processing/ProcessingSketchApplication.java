package kaleidok.processing;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kaleidok.javafx.PropertyLoaderApplication;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.stage.GeometryPreferences;
import kaleidok.javafx.stage.Screens;
import processing.core.PApplet;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;


public abstract class ProcessingSketchApplication<T extends PApplet>
  extends PropertyLoaderApplication
{
  private T sketch = null;

  @SuppressWarnings("StaticFieldReferencedViaSubclass")
  private final GeometryPreferences geometryPreferences =
    new GeometryPreferences(this, true, GeometryPreferences.POSITION);


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
      throw new IllegalStateException(
        sketch.getClass().getCanonicalName() + " is already initialized");
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
    geometryPreferences.applyGeometryAndBind(stage);
    stage.setScene(getScene());
    show(stage);
  }


  @SuppressWarnings("ProhibitedExceptionDeclared")
  protected void show( Stage stage ) throws Exception
  {
    stage.show();
  }


  @Override
  public void stop() throws Exception
  {
    sketch.exit();
  }


  protected Stream<? extends ReadOnlyPropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return geometryPreferences.getPreferenceAdapters();
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
        extSketch.getWindowBounds(), padding, preferredSides);
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


  @SuppressWarnings("MethodMayBeStatic")
  public void exit()
  {
    Platform.exit();
  }
}
