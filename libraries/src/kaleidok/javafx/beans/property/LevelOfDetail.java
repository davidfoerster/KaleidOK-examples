package kaleidok.javafx.beans.property;

import java.io.Serializable;
import java.util.Comparator;


public interface LevelOfDetail
{
  int getLevelOfDetail();


  class LevelOfDetailComparator
    implements Comparator<LevelOfDetail>, Serializable
  {
    private static final long serialVersionUID = -4550758895515201150L;

    public static final LevelOfDetailComparator INSTANCE = new LevelOfDetailComparator();


    protected LevelOfDetailComparator() { }


    @Override
    public int compare( LevelOfDetail lod1, LevelOfDetail lod2 )
    {
      return Integer.compare(lod1.getLevelOfDetail(), lod2.getLevelOfDetail());
    }
  }


  class DefaultLevelOfDetailComparator<T>
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
      return (o instanceof LevelOfDetail) ?
        ((LevelOfDetail) o).getLevelOfDetail() :
        defaultLevel;
    }
  }
}
