package kaleidok.javafx.stage;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.stage.Screen;
import kaleidok.javafx.geometry.Rectangles;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;


public final class Screens
{
  private Screens() { }


  private static final Side[] DEFAULT_PREFERRED_SIDES = {
      Side.RIGHT, Side.BOTTOM, Side.LEFT, Side.TOP
    };


  public static Map.Entry<Side, Point2D> placeAround( double windowWidth,
    double windowHeight, Rectangle2D occupiedArea, double padding,
    Side... preferredSides )
  {
    Rectangle2D totalScreenSpace =
      Screen.getScreens().stream().map(Screen::getVisualBounds)
        .reduce(Rectangles::union)
        .orElse(Rectangle2D.EMPTY);
    if (Rectangle2D.EMPTY.equals(totalScreenSpace))
      return null;

    if (preferredSides == null || preferredSides.length == 0)
      preferredSides = DEFAULT_PREFERRED_SIDES;

    for (Side side: preferredSides)
    {
      double x = occupiedArea.getMinX(), y = occupiedArea.getMinY();

      switch (side)
      {
      case LEFT:
        x -= windowWidth + padding;
        break;

      case RIGHT:
        x += occupiedArea.getWidth() + padding;
        break;

      case TOP:
        y -= windowHeight + padding;
        break;

      case BOTTOM:
        y += occupiedArea.getHeight() + padding;
        break;
      }

      x = Math.max(x, totalScreenSpace.getMinX());
      y = Math.max(y, totalScreenSpace.getMinY());
      if (!occupiedArea.intersects(x, y, windowWidth, windowHeight) &&
        totalScreenSpace.contains(x, y, windowWidth, windowHeight))
      {
        return new AbstractMap.SimpleEntry<>(side, new Point2D(x, y));
      }
    }

    return null;
  }


  public static Map.Entry<Side, Screen> getNeighborScreen(
    final int screenIdx, Side... preferredSides )
  {
    final List<Screen> screens = Screen.getScreens();
    final Screen referenceScreen = screens.get(screenIdx);
    if (screens.size() == 1)
      return null;

    if (preferredSides == null || preferredSides.length == 0)
      preferredSides = DEFAULT_PREFERRED_SIDES;

    for (Side side : preferredSides)
    {
      SideBoundsDistanceFunction distanceFunction =
        SideBoundsDistanceFunction.forSide(side, referenceScreen);
      Optional<Screen> closestScreen =
        screens.stream()
          .filter(distanceFunction)
          .min(Comparator.comparingDouble(distanceFunction));
      if (closestScreen.isPresent())
        return new AbstractMap.SimpleEntry<>(side, closestScreen.get());
    }

    return null;
  }


  private abstract static class SideBoundsDistanceFunction
    implements Predicate<Screen>, ToDoubleFunction<Screen>
  {
    protected final Screen referenceScreen;

    protected final Rectangle2D referenceBounds;


    protected SideBoundsDistanceFunction( Screen referenceScreen )
    {
      this.referenceScreen = referenceScreen;
      this.referenceBounds = referenceScreen.getBounds();
    }


    @Override
    public boolean test( Screen screen )
    {
      return screen != referenceScreen && distanceTo(screen) >= 0;
    }


    @Override
    public double applyAsDouble( Screen screen )
    {
      double d = distanceTo(screen);
      return (d >= 0) ? d : Double.POSITIVE_INFINITY;
    }


    public abstract double distanceTo( Screen screen );


    public static SideBoundsDistanceFunction forSide( Side side,
      Screen referenceScreen )
    {
      switch (side)
      {
      case LEFT:
        return new LeftSideBoundsDistanceFunction(referenceScreen);
      case RIGHT:
        return new RightSideBoundsDistanceFunction(referenceScreen);
      case TOP:
        return new TopSideBoundsDistanceFunction(referenceScreen);
      case BOTTOM:
        return new BottomSideBoundsDistanceFunction(referenceScreen);
      default:
        throw new IllegalArgumentException(String.valueOf(side));
      }
    }
  }


  private static class LeftSideBoundsDistanceFunction
    extends SideBoundsDistanceFunction
  {
    public LeftSideBoundsDistanceFunction( Screen referenceScreen )
    {
      super(referenceScreen);
    }

    @Override
    public double distanceTo( Screen screen )
    {
      return referenceBounds.getMinX() - screen.getBounds().getMaxX();
    }
  }


  private static class RightSideBoundsDistanceFunction
    extends SideBoundsDistanceFunction
  {
    public RightSideBoundsDistanceFunction( Screen referenceScreen )
    {
      super(referenceScreen);
    }

    @Override
    public double distanceTo( Screen screen )
    {
      return screen.getBounds().getMinX() - referenceBounds.getMaxX();
    }
  }


  private static class TopSideBoundsDistanceFunction
    extends SideBoundsDistanceFunction
  {
    public TopSideBoundsDistanceFunction( Screen referenceScreen )
    {
      super(referenceScreen);
    }

    @Override
    public double distanceTo( Screen screen )
    {
      return referenceBounds.getMinY() - screen.getBounds().getMaxY();
    }
  }


  private static class BottomSideBoundsDistanceFunction
    extends SideBoundsDistanceFunction
  {
    public BottomSideBoundsDistanceFunction( Screen referenceScreen )
    {
      super(referenceScreen);
    }

    @Override
    public double distanceTo( Screen screen )
    {
      return screen.getBounds().getMinY() - referenceBounds.getMaxY();
    }
  }
}
