package kaleidok.processing.image;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;


/**
 * <p>Used in {@link PImages#from(Image)}, otherwise rather pointless.
 *
 * <p>TODO: see {@link PImages#from(Image)}’s TODO
 *
 * @see PImages#from(Image)
 */
public class ProxyImage extends Image
{
  public final Image underlying;


  public ProxyImage( Image underlying )
  {
    this.underlying = underlying;
  }


  @Override
  public int getWidth( ImageObserver observer )
  {
    return underlying.getWidth(observer);
  }

  @Override
  public int getHeight( ImageObserver observer )
  {
    return underlying.getHeight(observer);
  }

  @Override
  public ImageProducer getSource()
  {
    return underlying.getSource();
  }

  @Override
  public Graphics getGraphics()
  {
    return underlying.getGraphics();
  }

  @Override
  public Object getProperty( String name, ImageObserver observer )
  {
    return underlying.getProperty(name, observer);
  }

  @Override
  public Image getScaledInstance( int width, int height, int hints )
  {
    return underlying.getScaledInstance(width, height, hints);
  }

  @Override
  public void flush()
  {
    underlying.flush();
  }

  @Override
  public ImageCapabilities getCapabilities( GraphicsConfiguration gc )
  {
    return underlying.getCapabilities(gc);
  }

  @Override
  public void setAccelerationPriority( float priority )
  {
    underlying.setAccelerationPriority(priority);
  }

  @Override
  public float getAccelerationPriority()
  {
    return underlying.getAccelerationPriority();
  }
}
