package kaleidok.processing.image;

import kaleidok.util.logging.LoggingUtils;
import processing.core.PImage;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CallablePImageReaderBase implements Callable<PImage>
{

  public static class Resources
  {
    public final ImageInputStream sourceStream;

    public final ImageReader imageReader;


    public Resources( ImageInputStream sourceStream, ImageReader imageReader )
    {
      this.sourceStream = Objects.requireNonNull(sourceStream);
      this.imageReader = Objects.requireNonNull(imageReader);
    }
  }


  private final AtomicReference<Resources> resources = new AtomicReference<>();


  protected CallablePImageReaderBase() { }


  public CallablePImageReaderBase( ImageInputStream sourceStream,
    ImageReader imageReader )
  {
    this();
    initFrom(sourceStream, imageReader);
  }


  public static CallablePImageReaderBase getInstance(
    ImageInputStream sourceStream )
  {
    CallablePImageReaderBase instance = new CallablePImageReaderBase();
    instance.initFrom(sourceStream, null, null);
    return instance;
  }


  public static CallablePImageReaderBase getInstance( InputStream is )
    throws IOException
  {
    return getInstance(ImageIO.createImageInputStream(is));
  }


  private IllegalStateException getAlreadyInitializedException()
  {
    return new IllegalStateException(
      this + " instance has already been initialized");
  }


  protected final void checkInitializable() throws IllegalStateException
  {
    if (resources.get() != null)
      throw getAlreadyInitializedException();
  }


  protected final void initFrom( ImageInputStream sourceStream,
    ImageReader imageReader )
    throws IllegalStateException
  {
    if (!resources.compareAndSet(
      null, new Resources(sourceStream, imageReader)))
    {
      throw getAlreadyInitializedException();
    }
  }


  protected void prepare() throws IOException { }


  @Override
  public PImage call() throws IOException, IllegalStateException
  {
    prepare();

    Resources res = this.resources.get();
    if (res == null)
      throw new IllegalStateException(this + " hasn't been initialized");

    Image img;
    try
    {
      img = PImages.getSuitableImage(res.imageReader, res.sourceStream);
    }
    finally
    {
      dispose();
    }
    return PImages.from(img);
  }


  @OverridingMethodsMustInvokeSuper
  protected void dispose()
  {
    Resources res = this.resources.getAndSet(null);
    if (res != null)
    {
      res.imageReader.dispose();

      try
      {
        res.sourceStream.close();
      }
      catch (IOException ex)
      {
        LoggingUtils.logThrown(
          Logger.getLogger(getClass().getName()), Level.WARNING,
          "Error while closing image source stream {0}",
          ex, res.sourceStream);
      }
    }
  }


  @OverridingMethodsMustInvokeSuper
  public void abort()
  {
    Resources res = this.resources.get();
    if (res != null) try
    {
      res.imageReader.abort();
    }
    catch (IllegalStateException ignored)
    {
      /*
       * Some image readers cannot be aborted from a different thread, so we
       * add a progress listener that calls abort from the image reader thread.
       */
      res.imageReader.addIIOReadProgressListener(
        IIOReadProgressAbortListener.INSTANCE);
    }
  }


  private PImageFutureTask future = null;

  public synchronized RunnableFuture<PImage> asFuture()
  {
    if (future == null)
      future = new PImageFutureTask();
    return future;
  }


  protected void initFrom( ImageInputStream iis, String mimeType,
    String fileExtension )
    throws IllegalStateException
  {
    checkInitializable();

    ImageReader imageReader = Objects.requireNonNull(
      PImages.getSuitableReader(Objects.requireNonNull(iis), mimeType, fileExtension));
    try
    {
      initFrom(iis, imageReader);
      imageReader = null;
    }
    finally
    {
      if (imageReader != null)
        imageReader.dispose();
    }
  }


  public Object getSource()
  {
    Resources res = this.resources.get();
    return (res != null) ? res.sourceStream.toString() : "no source";
  }


  @Override
  public String toString()
  {
    return "Callable<PImage> based on " + getSource();
  }


  protected class PImageFutureTask extends FutureTask<PImage>
  {
    protected PImageFutureTask()
    {
      super(CallablePImageReaderBase.this);
    }


    @Override
    public String toString()
    {
      return "FutureTask<PImage> of " + CallablePImageReaderBase.this;
    }


    @Override
    protected void done()
    {
      dispose();
    }


    @Override
    public boolean cancel( boolean mayInterruptIfRunning )
    {
      if (mayInterruptIfRunning)
      {
        Resources res = resources.get();
        if (res != null)
          res.imageReader.abort();
      }
      return super.cancel(mayInterruptIfRunning);
    }
  }
}
