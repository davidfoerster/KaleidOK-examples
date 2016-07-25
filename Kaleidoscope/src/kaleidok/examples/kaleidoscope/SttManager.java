package kaleidok.examples.kaleidoscope;

import kaleidok.google.speech.RecorderIcon;
import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import kaleidok.google.speech.Transcription;
import kaleidok.util.concurrent.AbstractFutureCallback;
import kaleidok.processing.Plugin;
import kaleidok.util.DefaultValueParser;
import processing.event.KeyEvent;

import java.applet.Applet;
import java.lang.reflect.InvocationTargetException;
import java.text.Format;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;


public class SttManager extends Plugin<Kaleidoscope>
{
  private final STT stt;


  SttManager( Kaleidoscope sketch )
  {
    super(sketch);

    stt = new STT(new SttResponseHandler(),
      sketch.parseStringOrFile(sketch.getParameter("com.google.developer.api.key"), '@'));
    String paramBase = stt.getClass().getCanonicalName() + '.';
    stt.setLanguage(sketch.getParameter(paramBase + "language", "en"));
    stt.setMaxTranscriptionInterval(
      DefaultValueParser.parseInt(sketch, paramBase + "interval", 8000),
      TimeUnit.MILLISECONDS);
    stt.intervalSequenceCountMax =
      DefaultValueParser.parseInt(sketch, paramBase + "interval.count",
        stt.intervalSequenceCountMax);
    stt.logfilePattern = getLogfilePattern();
    sketch.getAudioProcessingManager().getAudioDispatcher()
      .addAudioProcessor(stt.getAudioProcessor());

    RecorderIcon.fromConfiguration(sketch, stt, !STT.isLoggingStatus());
  }


  private Format getLogfilePattern()
  {
    String logFilePattern = p.getParameter(
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


  static boolean getParamIgnoreTranscriptionResult( Applet parent )
  {
    boolean isIgnoreTranscriptionResult =
      DefaultValueParser.parseBoolean(parent,
        parent.getClass().getPackage().getName() + ".ignoreTranscription",
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
}
