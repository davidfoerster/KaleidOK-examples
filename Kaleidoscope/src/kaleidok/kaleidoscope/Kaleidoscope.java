package kaleidok.kaleidoscope;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.processing.ProcessingSketchApplication;
import kaleidok.util.DefaultValueParser;
import processing.event.KeyEvent;

import java.util.Optional;
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

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<ExportService> exportService;


  public Kaleidoscope( ProcessingSketchApplication<Kaleidoscope> parent )
  {
    super(parent);
  }


  @Override
  public void settings()
  {
    size(1000, 1000, P3D);
    smooth(4);
    super.settings();
  }


  @Override
  public void setup()
  {
    super.setup();

    textureMode(NORMAL); // set texture coordinate mode to NORMALIZED (0 to 1)
    ellipseMode(RADIUS);
    noiseDetail(4, 0.6f);

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


  public SttManager getSTT()
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


  private Optional<ExportService> getExportService()
  {
    if (exportService == null)
    {
      exportService = Optional.ofNullable(
        ExportService.fromConfiguration(this, getParameterMap()));
      if (exportService.isPresent())
      {
        getChromasthetiationService()
          .setImageQueueCompletionCallback(exportService.get().getCallback());
      }
    }
    return exportService;
  }


  @Override
  public void draw()
  {
    layers.run();
  }


  @Override
  public void keyTyped( KeyEvent ev )
  {
    switch (ev.getKey())
    {
    case 'r':
      Optional<ExportService> es = getExportService();
      if (es.isPresent())
      {
        es.get().schedule();
        return;
      }
      break;
    }

    super.keyTyped(ev);
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
}
