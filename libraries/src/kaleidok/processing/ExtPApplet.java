package kaleidok.processing;

import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;
import processing.core.PImage;

import javax.swing.JApplet;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


/**
 * This is an intermediary class to "enrich" PApplet.
 */
public class ExtPApplet extends PApplet
{
  private JApplet parent;

  public ExtPApplet( JApplet parent )
  {
    this.parent = parent;
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
      if (documentBase.getProtocol().equals("file")) {
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
      if (url.getProtocol().equals("file")) {
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

    default:
      if (resizeMode == null) {
        throw new NullPointerException("mode");
      } else {
        // If this section is ever reached, somebody forgot to implement a case of this switch block.
        throw new AssertionError();
      }
    }

    image(img, dstLeft, dstTop, dstWidth, dstHeight, srcLeft, srcTop, srcWidth, srcHeight);
  }
}
