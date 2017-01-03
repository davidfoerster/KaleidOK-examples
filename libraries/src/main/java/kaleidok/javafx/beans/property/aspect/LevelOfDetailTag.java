package kaleidok.javafx.beans.property.aspect;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.value.ObservableIntegerValue;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;

import java.io.Serializable;
import java.util.Comparator;


public final class LevelOfDetailTag<T>
  extends InstantiatingPropertyAspectTag<ReadOnlyIntegerWrapper,T>
{
  private static final LevelOfDetailTag<?> INSTANCE = new LevelOfDetailTag<>();


  @SuppressWarnings("unchecked")
  public static <T> LevelOfDetailTag<T> getInstance()
  {
    return (LevelOfDetailTag<T>) INSTANCE;
  }


  private LevelOfDetailTag() { }


  @Override
  public ReadOnlyIntegerWrapper setup( AspectedReadOnlyProperty<? extends T> property )
  {
    return new ReadOnlyIntegerWrapper(property, "level of detail");
  }


  public static class DefaultLevelOfDetailComparator<T>
    implements Comparator<T>, Serializable
  {
    private static final long serialVersionUID = -1718493316325802502L;


    public final int defaultLevel;


    public DefaultLevelOfDetailComparator( int defaultLevel )
    {
      this.defaultLevel = defaultLevel;
    }


    @Override
    public int compare( T o1, T o2 )
    {
      if (o1 == null || o2 == null)
        throw new NullPointerException();

      return Integer.compare(getLevelOfDetail(o1), getLevelOfDetail(o2));
    }


    public int getLevelOfDetail( Object o )
    {
      if (o instanceof AspectedReadOnlyProperty)
      {
        ObservableIntegerValue lod =
          ((AspectedReadOnlyProperty<?>) o).getAspect(getInstance());
        if (lod != null)
          return lod.get();
      }
      return defaultLevel;
    }
  }
}
