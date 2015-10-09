package kaleidok.examples.kaleidoscope;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.util.DefaultValueParser;

import javax.swing.JApplet;

import java.awt.Point;

import static kaleidok.util.DebugManager.verbose;


public class Kaleidoscope extends ExtPApplet
{
  /**
   * Manages the kaleidoscopic layers of shapes of this sketch.
   */
  private LayerManager layers;

  /**
   * Manages the source of an audio signal and its processing and possibly
   * playback.
   */
  private AudioProcessingManager audioProcessingManager;

  /**
   *
   */
  private KaleidoscopeChromasthetiationService chromasthetiationService;

  /**
   * Manages the transcription of an audio signal with the "Speech-to-Text"
   * module.
   */
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
    layers.run();
    previousSize.setLocation(width, height);
  }


  /**
   * Tells, whether the size of this sketch changed since the last frame.
   *
   * @return <code>true</code>, if changed; <code>false</code> otherwise
   */
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


  /**
   * If the supplied string <code>s</code> begins with <code>filePrefix</code>,
   * its remainder is interpreted as a path to a file, whose content shall be
   * returned. The special path <code>"-"</code> is interpreted as the standard
   * input stream of this process. The default encoding of this runtime
   * environment is used to decode the bytes of that stream into a string and
   * all terminating platform-specific line separators are removed from its
   * end.
   * <p>
   * In all other cases the string itself is returned.
   *
   * @param s  A string
   * @param filePrefix  A character to start a file path
   * @return  The same or another string
   */
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
