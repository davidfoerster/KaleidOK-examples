package kaleidok.processing;

import kaleidok.awt.ImageIO;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.JApplet;
import javax.swing.SwingWorker;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private JApplet parent;

  public ExtPApplet( JApplet parent )
  {
    this.parent = parent;
    registerMethod("post", this);
  }


  @Override
  public void destroy()
  {
    unregisterMethod("post", this);
    super.destroy();
  }


  @SuppressWarnings("unused")
  public void post()
  {
    saveFilenames.handleEntries();
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


  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final ImageSaveList saveFilenames = new ImageSaveList();


  public void save( String filename, boolean fullFrame )
  {
    if (fullFrame) {
      synchronized (saveFilenames) {
        saveFilenames.add(filename);
      }
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
    (new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws IOException
      {
        if ((g.format == RGB || g.format == ARGB) && filename.endsWith(".bmp")) {
          Path filePath = Paths.get(savePath(filename));
          try {
            ImageIO.saveBmp32(filePath, width, height, pixels, 0).force();
            return null;
          } catch (UnsupportedOperationException ignored) {
            // try again with default code path
          }
        }

        ExtPApplet.super.save(filename);
        return null;
      }
    }).execute();
  }


  @SuppressWarnings("serial")
  private class ImageSaveList extends HashSet<String>
  {
    @Override
    public synchronized boolean add( String s )
    {
      return super.add(s);
    }

    public void handleEntries()
    {
      if (!isEmpty()) {
        synchronized (this) {
          if (!isEmpty()) {
            loadPixels();
            for (String fn : this)
              saveImpl(fn);
            clear();
          }
        }
      }
    }
  }
}
