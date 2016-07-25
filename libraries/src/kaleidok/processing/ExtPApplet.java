package kaleidok.processing;

import kaleidok.awt.ImageIO;
import kaleidok.util.DefaultValueParser;
import kaleidok.util.Threads;
import kaleidok.util.concurrent.GroupedThreadFactory;
import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.JApplet;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_STRING_ARRAY;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private JApplet parent;

  public final Set<String> saveFilenames = new ImageSaveSet(this);

  protected ExecutorService executorService;

  public ExtPApplet( JApplet parent )
  {
    this.parent = parent;
  }


  @OverridingMethodsMustInvokeSuper
  public void settings()
  {
    executorService = new ThreadPoolExecutor(
      0, 16, 60, TimeUnit.SECONDS, new SynchronousQueue<>(),
      new GroupedThreadFactory(
        getClass().getSimpleName() + " worker pool", true));
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void setup()
  {
    try
    {
      /*
       * Older Processing releases don't have a "settings" method that they
       * call on initiation, so we do it for them.
       *
       * TODO: Remove this check once the transition to such a version is
       * complete.
       */
      PApplet.class.getMethod("settings", EMPTY_CLASS_ARRAY);
    }
    catch (NoSuchMethodException ignored)
    {
      settings();
    }
  }


  @Override
  @OverridingMethodsMustInvokeSuper
  public void dispose()
  {
    saveFilenames.clear();
    if (executorService != null)
      executorService.shutdownNow();

    super.dispose();
  }


  @Override
  public String getParameter( String name )
  {
    return parent.getParameter(name);
  }

  public <T> T getParameter( String name, T defaultValue )
  {
    return DefaultValueParser.parse(getParameter(name), defaultValue);
  }


  private URL documentBase = null;

  @Override
  public URL getDocumentBase()
  {
    if (documentBase == null) {
      URL documentBase = parent.getDocumentBase();
      if ("file".equals(documentBase.getProtocol())) {
        documentBase = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        try {
          documentBase = new URL(documentBase, documentBase.getPath() + "data/");
        } catch (MalformedURLException ex) {
          throw new AssertionError(ex);
        }
      }
      this.documentBase = documentBase;
    }
    return documentBase;
  }

  @Override
  public Image getImage( URL url )
  {
    return parent.getImage(url);
  }

  @Override
  public Image getImage( URL url, String name )
  {
    return parent.getImage(url, name);
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    for (KeyListener listener: getKeyListeners()) {
      if (listener != this) {
        listener.keyPressed(e);
        if (e.isConsumed())
          return;
      }
    }
    super.keyPressed(e);
  }


  public PImageFuture getImageFuture( String path )
  {
    URL url = this.getClass().getResource(path);
    if (url == null) {
      try {
        url = new URL(getDocumentBase(), path);
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(ex);
      }
      if ("file".equals(url.getProtocol())) {
        File file;
        try {
          file = new File(url.toURI());
        } catch (URISyntaxException ex) {
          throw new AssertionError(ex);
        }
        if (!file.isFile() || !file.canRead())
          url = null;
      }
    }
    return (url != null) ? getImageFuture(url) : null;
  }


  public PImageFuture getImageFuture( URL url )
  {
    return getImageFuture(url, -1, -1);
  }


  public PImageFuture getImageFuture( URL url, int width, int height )
  {
    return new PImageFuture(this, getImage(url), width, height);
  }


  public enum ImageResizeMode
  {
    STRETCH,
    PAN
  }

  public void image( PImage img, ImageResizeMode resizeMode,
    float dstLeft, float dstTop, float dstWidth, float dstHeight )
  {
    int srcLeft = 0, srcTop = 0, srcWidth = img.width, srcHeight = img.height;

    if (dstWidth <= 0 || dstHeight <= 0) {
      throw new IllegalArgumentException("Image destination has zero area");
    }
    if (srcWidth <= 0 || srcHeight <= 0) {
      throw new IllegalArgumentException("Image source has zero area");
    }
    if (g.imageMode != CORNER) {
      throw new UnsupportedOperationException(
        "Image modes besides CORNER are currently unimplemented");
    }

    switch (resizeMode) {
    case STRETCH:
      break;

    case PAN:
      double dstRatio = (double) dstWidth / dstHeight,
        srcRatio = (double) srcWidth / srcHeight;
      if (srcRatio > dstRatio) {
        srcWidth = (int)(srcHeight * dstRatio + 0.5);
        assert srcWidth <= img.width : srcWidth + " > " + img.width;
        srcLeft = (img.width - srcWidth) / 2;
      } else if (srcRatio < dstRatio) {
        srcHeight = (int)(srcWidth / dstRatio + 0.5);
        assert srcHeight <= img.height : srcHeight + " > " + img.height;
        srcTop = (img.height - srcHeight) / 2;
      }
      break;
    }

    image(img, dstLeft, dstTop, dstWidth, dstHeight, srcLeft, srcTop, srcWidth, srcHeight);
  }


  public void save( String filename, boolean fullFrame )
  {
    if (fullFrame) {
      saveFilenames.add(filename);
    } else {
      save(filename);
    }
  }


  @Override
  public void save( String filename )
  {
    loadPixels();
    saveImpl(filename);
  }


  protected void saveImpl( final String filename )
  {
    thread(() ->
    {
      if ((g.format == RGB || g.format == ARGB) && filename.endsWith(".bmp"))
      {
        Path filePath = Paths.get(savePath(filename), EMPTY_STRING_ARRAY);
        try {
          ImageIO.saveBmp32(filePath, width, height, pixels, 0).force();
        } catch (UnsupportedOperationException ignored) {
          // try again with default code path
        }
        catch (IOException ex)
        {
          Threads.handleUncaught(ex);
        }
      }

      ExtPApplet.super.save(filename);
    });
  }


  public static class ImageSaveSet
    extends Plugin<ExtPApplet> implements Set<String>
  {
    private final HashSet<String> underlying = new HashSet<>();


    private ImageSaveSet( ExtPApplet parent )
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
            p.loadPixels();
            for (String fn : this)
              p.saveImpl(fn);
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


  public <R extends Runnable> R thread( R action )
  {
    executorService.execute(action);
    return action;
  }


  @Override
  public void thread( String methodName )
  {
    final Method method;
    try
    {
      method = getClass().getMethod(methodName, EMPTY_CLASS_ARRAY);
    }
    catch (NoSuchMethodException ex)
    {
      throw new AssertionError(ex);
    }

    thread(() ->
    {
      try
      {
        method.invoke(ExtPApplet.this, EMPTY_OBJECT_ARRAY);
      }
      catch (IllegalAccessException | IllegalArgumentException ex)
      {
        throw new AssertionError(ex);
      }
      catch (InvocationTargetException ex)
      {
        Threads.handleUncaught(ex);
      }
    });
  }
}
