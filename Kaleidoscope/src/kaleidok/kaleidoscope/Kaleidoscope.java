package kaleidok.kaleidoscope;

import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.processing.ProcessingSketchApplication;
import processing.event.KeyEvent;

import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class Kaleidoscope extends ExtPApplet
  implements PreferenceBean
{
  static final Logger logger =
    Logger.getLogger(Kaleidoscope.class.getPackage().getName());

  /**
   * Manages the kaleidoscopic layers of shapes of this sketch.
   */
  private volatile LayerManager layers;

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
  private volatile SttManager stt;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<ExportService> exportService;

  private FrameRateDisplay frameRateDisplay;


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
    getFrameRateDisplay();
  }


  @Override
  public synchronized void dispose()
  {
    saveAndFlush();

    if (layers != null)
      layers.dispose();
    if (chromasthetiationService != null)
      chromasthetiationService.dispose();

    super.dispose();
  }


  public synchronized LayerManager getLayers()
  {
    if (layers == null)
      layers = new LayerManager(this);
    return layers;
  }


  public synchronized AudioProcessingManager getAudioProcessingManager()
  {
    if (audioProcessingManager == null)
      audioProcessingManager = new AudioProcessingManager(this);
    return audioProcessingManager;
  }


  public synchronized SttManager getSTT()
  {
    SttManager stt = this.stt;
    if (stt == null)
      this.stt = stt = new SttManager(this);
    return stt;
  }


  public synchronized KaleidoscopeChromasthetiationService getChromasthetiationService()
  {
    if (chromasthetiationService == null)
      chromasthetiationService = KaleidoscopeChromasthetiationService.newInstance(this);
    return chromasthetiationService;
  }


  private synchronized Optional<ExportService> getExportService()
  {
    if (this.exportService == null)
    {
      final ExportService exportService =
        ExportService.fromConfiguration(this, getParameterMap());
      this.exportService = Optional.ofNullable(exportService);
      if (exportService != null)
      {
        getChromasthetiationService().imageQueueCompletionCallback =
          (text, photos) -> exportService.getCallback().accept(text);
      }
    }
    return this.exportService;
  }


  public synchronized FrameRateDisplay getFrameRateDisplay()
  {
    if (frameRateDisplay == null)
    {
      frameRateDisplay = new FrameRateDisplay(this, 0);
      frameRateDisplay.getPreferenceAdapters()
        .forEach(PropertyPreferencesAdapter::load);
    }
    return frameRateDisplay;
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
      String suffix = System.lineSeparator();
      int suffixLen = suffix.length();
      if (suffixLen > 0)
      {
        int sLen = s.length();
        while (s.startsWith(suffix, sLen - suffixLen))
          sLen -= suffixLen;
        s = s.substring(0, sLen);
      }
    }
    return s;
  }


  @Override
  public String getName()
  {
    return "Kaleidoscope";
  }


  @Override
  public Object getParent()
  {
    return null;
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    Stream<Stream<? extends PropertyPreferencesAdapter<?,?>>> s = Stream.of(
      getSTT().getPreferenceAdapters(),
      getChromasthetiationService().getPreferenceAdapters(),
      getLayers().getPreferenceAdapters(),
      getFrameRateDisplay().getPreferenceAdapters());

    return Stream.concat(
      s.flatMap(Function.identity()),
      super.getPreferenceAdapters());
  }
}
