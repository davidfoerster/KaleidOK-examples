package kaleidok.processing.export;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public final class ImageSaveSet
  extends Plugin<ExtPApplet> implements Set<String>
{
  private final HashSet<String> underlying = new HashSet<>();


  public ImageSaveSet( ExtPApplet parent )
  {
    super(parent);
  }


  @Override
  public void post()
  {
    if (!isEmpty())
    {
      synchronized (this)
      {
        if (!isEmpty())
        {
          final ExtPApplet p = this.p;
          for (String fn : this)
            p.save(fn);
          clear();
        }
      }
    }
  }


  @Override
  public void dispose()
  {
    clear();
    super.dispose();
  }


  @Override
  public int size()
  {
    return underlying.size();
  }

  @Override
  public boolean isEmpty()
  {
    return underlying.isEmpty();
  }

  @Override
  public synchronized boolean contains( Object o )
  {
    return underlying.contains(o);
  }

  @Override
  public Iterator<String> iterator()
  {
    return underlying.iterator();
  }

  @Override
  public synchronized Object[] toArray()
  {
    return underlying.toArray();
  }

  @SuppressWarnings("SuspiciousToArrayCall")
  @Override
  public synchronized <T> T[] toArray( T[] a )
  {
    return underlying.toArray(a);
  }

  @Override
  public synchronized boolean add( String s )
  {
    return underlying.add(s);
  }

  @Override
  public synchronized boolean remove( Object o )
  {
    return underlying.remove(o);
  }

  @Override
  public synchronized boolean containsAll( Collection<?> c )
  {
    return underlying.containsAll(c);
  }

  @Override
  public synchronized boolean addAll( Collection<? extends String> c )
  {
    return underlying.addAll(c);
  }

  @Override
  public synchronized boolean retainAll( Collection<?> c )
  {
    return underlying.retainAll(c);
  }

  @Override
  public synchronized boolean removeAll( Collection<?> c )
  {
    return underlying.removeAll(c);
  }

  @Override
  public synchronized void clear()
  {
    underlying.clear();
  }

  @Override
  public synchronized boolean equals( Object o )
  {
    return underlying.equals(
      (o instanceof ImageSaveSet) ?
        ((ImageSaveSet) o).underlying :
        o);
  }

  @Override
  public synchronized int hashCode()
  {
    return underlying.hashCode();
  }
}
