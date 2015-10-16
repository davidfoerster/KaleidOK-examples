package kaleidok.examples.kaleidoscope;

import com.getflourish.stt2.RecorderIcon;
import com.getflourish.stt2.STT;
import com.getflourish.stt2.SttResponse;
import com.getflourish.stt2.Transcription;
import kaleidok.concurrent.AbstractFutureCallback;
import kaleidok.util.DefaultValueParser;

import java.text.Format;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static kaleidok.examples.kaleidoscope.Kaleidoscope.logger;


public class SttManager
{
  private final Kaleidoscope parent;

  private final STT stt;


  SttManager( Kaleidoscope parent )
  {
    this.parent = parent;

    stt = new STT(new SttResponseHandler(),
      parent.parseStringOrFile(parent.getParameter("com.google.developer.api.key"), '@'));
    String paramBase = stt.getClass().getCanonicalName() + '.';
    stt.setLanguage((String) parent.getParameter(paramBase + "language", "en"));
    stt.setMaxTranscriptionInterval(
      DefaultValueParser.parseInt(parent, paramBase + "interval", 8000),
      TimeUnit.MILLISECONDS);
    stt.intervalSequenceCountMax =
      DefaultValueParser.parseInt(parent, paramBase + "interval.count",
        stt.intervalSequenceCountMax);
    stt.logfilePattern = getLogfilePattern();
    parent.getAudioProcessingManager().getAudioDispatcher()
      .addAudioProcessor(stt.getAudioProcessor());

    initRecorderIcon();
  }


  private Format getLogfilePattern()
  {
    String logFilePattern = parent.getParameter(
      stt.getClass().getCanonicalName() + ".log.pattern");
    try {
      return (logFilePattern != null) ?
        Transcription.buildLogfileFormat(null, logFilePattern) : null;
    } catch (ReflectiveOperationException ex) {
      throw new IllegalArgumentException(
        "Cannot parse log file pattern: " + logFilePattern, ex);
    }
  }


  private boolean initRecorderIcon()
  {
    String strEnabled =
      parent.getParameter(RecorderIcon.class.getCanonicalName() + ".enabled");
    boolean bEnabled = !"forceoff".equals(strEnabled) &&
      (!STT.isLoggingStatus() ||
         DefaultValueParser.parseBoolean(strEnabled, true));

    if (bEnabled) {
      RecorderIcon ri = new RecorderIcon(parent, stt);
    }

    return bEnabled;
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
          parent.getChromasthetiationService()
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
          DefaultValueParser.parseBoolean(parent,
            parent.getClass().getPackage().getName() + ".ignoreTranscription",
            false);
        if (isIgnoreTranscriptionResult) {
          logger.config(
            "Speech transcription results are configured to be ignored");
        }
      }
      return isIgnoreTranscriptionResult;
    }
  }


  public void shutdown()
  {
    stt.shutdown();
  }

  public void begin( boolean doThrow )
  {
    stt.begin(doThrow);
  }

  public void end( boolean doThrow )
  {
    stt.end(doThrow);
  }
}
