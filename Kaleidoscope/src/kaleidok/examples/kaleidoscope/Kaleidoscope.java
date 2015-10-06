package kaleidok.examples.kaleidoscope;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.util.DefaultValueParser;

import javax.swing.JApplet;

import java.awt.Point;

import static kaleidok.util.DebugManager.verbose;


public class Kaleidoscope extends ExtPApplet
{
  private LayerManager layers;

  private AudioProcessingManager audioProcessingManager;

  private KaleidoscopeChromasthetiationService chromasthetiationService;

  private SttManager stt;


  public Kaleidoscope( JApplet parent )
  {
    super(parent);
  }

  @Override
  public void setup()
  {
    Point size = new Point(width, height);
    if (size.x == 100 && size.y == 100) {
      /*
       * Default dimensions mean, the surrounding layout manager didn't resize
       * this sketch yet; use more sensible default dimensions instead (and set
       * them thereafter).
       */
      size.setLocation(1000, 1000);
    }
    this.size(size.x, size.y, OPENGL); // keep size, but use the OpenGL renderer
    previousSize = size;

    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    ellipseMode(RADIUS);

    int smoothingLevel = DefaultValueParser.parseInt(this,
      g.getClass().getCanonicalName() + ".smooth", 4);
    if (smoothingLevel > 0) {
      smooth(smoothingLevel);
    } else {
      noSmooth();
    }

    getLayers();
    getChromasthetiationService();
    getSTT();
    getAudioProcessingManager().getAudioDispatcherThread().start();

    if (verbose >= 1)
      new FrameRateDisplay(this);
  }


  public LayerManager getLayers()
  {
    if (layers == null)
      layers = new LayerManager(this);
    return layers;
  }


  public AudioProcessingManager getAudioProcessingManager()
  {
    if (audioProcessingManager == null)
      audioProcessingManager = new AudioProcessingManager(this);
    return audioProcessingManager;
  }


  private SttManager getSTT()
  {
    if (stt == null)
      stt = new SttManager(this);
    return stt;
  }


  public KaleidoscopeChromasthetiationService getChromasthetiationService()
  {
    if (chromasthetiationService == null)
      chromasthetiationService = KaleidoscopeChromasthetiationService.newInstance(this);
    return chromasthetiationService;
  }


  @Override
  public void destroy()
  {
    stt.shutdown();
    super.destroy();
  }


  private Point previousSize = null;

  @Override
  public void draw()
  {
    layers.draw();
    previousSize.setLocation(width, height);
  }


  public boolean wasResized()
  {
    final Point previousSize = this.previousSize;
    return previousSize != null &&
      (width != previousSize.x || height != previousSize.y);
  }


  @Override
  public void keyTyped()
  {
    switch (key) {
    case 'i':
      stt.begin(false);
      break;

    case 'o':
      stt.end(false);
      break;
    }
  }


  String parseStringOrFile( String s, char filePrefix )
  {
    if (s != null && !s.isEmpty() && s.charAt(0) == filePrefix) {
      s = new String((s.length() == 2 && s.charAt(1) == '-') ?
          loadBytes(System.in) : loadBytes(s.substring(1)));
      String ls = System.getProperty("line.separator");
      int sLen = s.length(), lsLen = ls.length();
      while (sLen >= lsLen && s.startsWith(ls, sLen - lsLen))
        sLen -= lsLen;
      s = s.substring(0, sLen);
    }
    return s;
  }

}
