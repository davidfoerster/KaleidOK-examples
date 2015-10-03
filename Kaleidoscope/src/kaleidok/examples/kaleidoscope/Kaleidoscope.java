package kaleidok.examples.kaleidoscope;

import kaleidok.processing.ExtPApplet;
import kaleidok.processing.FrameRateDisplay;
import kaleidok.util.DefaultValueParser;

import javax.swing.JApplet;

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
    int width = this.width, height = this.height;
    if (width == 100 && height == 100) {
      /*
       * Default dimensions mean, the surrounding layout manager didn't resize
       * this sketch yet; use more sensible default dimensions instead (and set
       * them thereafter).
       */
      width = 1000;
      height = 1000;
    }
    size(width, height, OPENGL); // keep size, but use the OpenGL renderer
    previousWidth = width;
    previousHeight = height;

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


  private int previousWidth = -1, previousHeight = -1;

  @Override
  public void draw()
  {
    getLayers().draw();
    previousWidth = width;
    previousHeight = height;
  }


  public boolean wasResized()
  {
    return width != previousWidth || height != previousHeight;
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
