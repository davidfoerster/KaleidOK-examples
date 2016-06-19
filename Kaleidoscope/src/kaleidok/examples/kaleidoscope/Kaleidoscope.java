package kaleidok.examples.kaleidoscope;

import kaleidok.util.containers.Reference;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.util.DefaultValueParser;
import processing.event.KeyEvent;

import javax.swing.JApplet;

import java.awt.Point;
import java.util.logging.Logger;


public class Kaleidoscope extends ExtPApplet
{
  static final Logger logger =
    Logger.getLogger(Kaleidoscope.class.getPackage().getName());

  /**
   * Manages the kaleidoscopic layers of shapes of this sketch.
   */
  private LayerManager layers;

  /**
   * Manages the source of an audio signal and its processing and possibly
   * playback.
   */
  private AudioProcessingManager audioProcessingManager;

  private KaleidoscopeChromasthetiationService chromasthetiationService;

  /**
   * Manages the transcription of an audio signal with the "Speech-to-Text"
   * module.
   */
  private SttManager stt;

  private Reference<ExportService> exportService;


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
    noiseDetail(4, 0.6f);

    frameRate((float) DefaultValueParser.parseDouble(this,
      "framerate", this.frameRate));

    smooth(DefaultValueParser.parseInt(this,
      g.getClass().getCanonicalName() + ".smooth", 4));

    getLayers();
    getChromasthetiationService();
    getSTT();
    getAudioProcessingManager().getAudioDispatcherThread().start();
    getExportService();
    FrameRateDisplay.fromConfiguration(this);
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


  private ExportService getExportService()
  {
    if (exportService == null)
    {
      exportService = new Reference<>(
        ExportService.fromConfiguration(this,
          (callback) ->
            getChromasthetiationService().
              setImageQueueCompletionCallback(callback)));
    }
    return exportService.item;
  }


  private Point previousSize = null;

  @Override
  public void draw()
  {
    layers.run();
    previousSize.setLocation(width, height);
  }


  @Override
  public void keyTyped( KeyEvent ev )
  {
    switch (ev.getKey())
    {
    case 'r':
      ExportService es = getExportService();
      if (es != null)
      {
        es.schedule();
        return;
      }
      break;
    }

    super.keyTyped(ev);
  }


  /**
   * Tells, whether the size of this sketch changed since the last frame.
   *
   * @return {@code true}, if changed; {@code false} otherwise
   */
  public boolean wasResized()
  {
    final Point previousSize = this.previousSize;
    return previousSize != null &&
      (width != previousSize.x || height != previousSize.y);
  }


  /**
   * If the supplied string {@code s} begins with {@code filePrefix}, its
   * remainder is interpreted as a path to a file, whose content shall be
   * returned. The special path {@code "-"} is interpreted as the standard
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
      assert !ls.isEmpty();
      int sLen = s.length(), lsLen = ls.length();
      while (sLen >= lsLen && s.startsWith(ls, sLen - lsLen))
        sLen -= lsLen;
      s = s.substring(0, sLen);
    }
    return s;
  }


  @Override
  public void smooth( int level )
  {
    if (level > 0) {
      super.smooth(level);
    } else {
      noSmooth();
    }
  }
}
