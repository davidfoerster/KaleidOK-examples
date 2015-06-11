package kaleidok.flickr;

import com.flickr4java.flickr.photos.Size;

import java.util.Comparator;


public class PhotoSizeComparator implements Comparator<Size>
{
  public static final PhotoSizeComparator INSTANCE = new PhotoSizeComparator();

  protected PhotoSizeComparator() { }

  @Override
  public int compare( Size o1, Size o2 )
  {
    return Integer.compare(getSize(o1), getSize(o2));
  }

  private static int getSize( Size o )
  {
    return o.getWidth() * o.getHeight();
  }
}
