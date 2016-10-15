package kaleidok.kaleidoscope;

import kaleidok.google.speech.RecorderIcon;
import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import kaleidok.google.speech.Transcription;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.processing.ExtPApplet;
import kaleidok.util.concurrent.AbstractFutureCallback;
import kaleidok.processing.Plugin;
import kaleidok.util.prefs.DefaultValueParser;
import processing.event.KeyEvent;

import java.lang.reflect.InvocationTargetException;
import java.text.Format;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;


public class SttManager extends Plugin<Kaleidoscope>
  implements PreferenceBean
{
  private final STT stt;


  SttManager( Kaleidoscope sketch )
  {
    super(sketch);

    Map<String, String> params = sketch.getParameterMap();
    stt = new STT(new SttResponseHandler(),
      sketch.parseStringOrFile(
        params.get("com.google.developer.api.key"), '@'));
    String paramBase = stt.getClass().getCanonicalName() + '.';
    stt.setLanguage(
      params.getOrDefault(paramBase + "language", "en"));
    stt.setMaxTranscriptionInterval(DefaultValueParser.parseInt(
      params.get(paramBase + "interval"), 8000),
      TimeUnit.MILLISECONDS);
    stt.intervalSequenceCountMax = DefaultValueParser.parseInt(
      params.get(paramBase + "interval.count"),
      stt.intervalSequenceCountMax);
    stt.logfilePattern = getLogfilePattern();
    sketch.getAudioProcessingManager().getAudioDispatcher()
      .addAudioProcessor(stt.getAudioProcessor());

    RecorderIcon.fromConfiguration(sketch, stt, !STT.isLoggingStatus());
  }


  private Format getLogfilePattern()
  {
    String logFilePattern = p.getParameterMap().get(
      stt.getClass().getCanonicalName() + ".log.pattern");
    try {
      return (logFilePattern != null) ?
        Transcription.buildLogfileFormat(null, logFilePattern) : null;
    } catch (ReflectiveOperationException ex) {
      throw new IllegalArgumentException(
        "Cannot parse log file pattern: " + logFilePattern,
        (ex instanceof InvocationTargetException) ? ex.getCause() : ex);
    }
  }


  private class SttResponseHandler extends AbstractFutureCallback<SttResponse>
  {
    private Boolean isIgnoreTranscriptionResult = null;


    @Override
    public void completed( SttResponse response )
    {
      SttResponse.Result.Alternative topAlternative =
        response.getTopAlternative();

      if (topAlternative != null) {
        logger.log(Level.INFO,
          "Transcribed with confidence {0,number,percent}: {1}",
          new Object[]{topAlternative.confidence, topAlternative.transcript});

        if (!isIgnoreTranscriptionResult()) {
          p.getChromasthetiationService()
            .submit(topAlternative.transcript);
        }
      } else {
        logger.info("Transcription returned no result");
      }
    }


    private boolean isIgnoreTranscriptionResult()
    {
      if (isIgnoreTranscriptionResult == null) {
        isIgnoreTranscriptionResult =
          getParamIgnoreTranscriptionResult(p);
      }
      return isIgnoreTranscriptionResult;
    }
  }


  static boolean getParamIgnoreTranscriptionResult( ExtPApplet parent )
  {
    @SuppressWarnings("SpellCheckingInspection")
    boolean isIgnoreTranscriptionResult =
      DefaultValueParser.parseBoolean(
        parent.getParameterMap().get(
          parent.getClass().getPackage().getName() + ".ignoretranscription"),
        false);
    if (isIgnoreTranscriptionResult) {
      logger.config(
        "Speech transcription results are configured to be ignored");
    }
    return isIgnoreTranscriptionResult;
  }


  public void shutdown()
  {
    stt.shutdown();
  }


  @Override
  public void dispose()
  {
    shutdown();
    super.dispose();
  }


  public void begin( boolean doThrow )
  {
    stt.begin(doThrow);
  }


  public void end( boolean doThrow )
  {
    stt.end(doThrow);
  }


  @Override
  public void keyEvent( KeyEvent ev )
  {
    if (ev.getAction() == KeyEvent.TYPE) {
      switch (ev.getKey()) {
      case 'i':
        begin(false);
        break;

      case 'o':
        end(false);
        break;
      }
    }
  }


  public void setRecorderStatus( boolean isRecording, boolean doThrow )
  {
    if (isRecording) {
      begin(doThrow);
    } else {
      end(doThrow);
    }
  }


  @Override
  public String getName()
  {
    return stt.getName();
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    return stt.getPreferenceAdapters();
  }
}
