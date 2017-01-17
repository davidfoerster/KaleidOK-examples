package kaleidok.kaleidoscope.layer.util;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableIntegerValue;


public class CircularTriangleStripSegmentCoordinatesBinding
  extends ObjectBinding<float[]>
{
  public static final int DIMENSIONS = 2;

  public static final int MIN_SEGMENTS = 3;

  public static final int MAX_SEGMENTS = Integer.MAX_VALUE / DIMENSIONS;


  public final ObservableIntegerValue segmentCount;


  public CircularTriangleStripSegmentCoordinatesBinding(
    ObservableIntegerValue segmentCount )
  {
    bind(segmentCount);
    this.segmentCount = segmentCount;
  }


  @Override
  protected float[] computeValue()
  {
    final float[] segmentCoords =
      new float[Math.multiplyExact(segmentCount.get(), DIMENSIONS)];
    final double step = Math.PI * 2 / segmentCoords.length;  // generate the step size based on the number of segments

    // pre-calculate x and y based on angle and store values interleaved in an array
    for (int i = segmentCoords.length - DIMENSIONS; i >= 0; i -= DIMENSIONS)
    {
      double θ = step * i; // angle for this segment
      segmentCoords[i] = (float) Math.sin(θ);
      segmentCoords[i + 1] = (float) Math.cos(θ);
    }

    return segmentCoords;
  }


  @Override
  public void dispose()
  {
    unbind(segmentCount);
  }
}
