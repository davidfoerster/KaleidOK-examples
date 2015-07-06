package kaleidok.processing;

import kaleidok.util.DebugManager;
import kaleidok.util.DefaultValueParser;
import processing.core.PApplet;

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
    if (parent != null)
      DebugManager.fromApplet(parent);
  }


  @Override
  public String getParameter( String name )
  {
    return parent.getParameter(name);
  }

  public Object getParameter( String name, Object defaultValue )
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
}
