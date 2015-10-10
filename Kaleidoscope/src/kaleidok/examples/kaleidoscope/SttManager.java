package kaleidok.examples.kaleidoscope;

import com.getflourish.stt2.RecorderIcon;
import com.getflourish.stt2.STT;
import com.getflourish.stt2.SttResponse;
import kaleidok.concurrent.AbstractFutureCallback;
import kaleidok.util.DefaultValueParser;

import java.util.concurrent.TimeUnit;

import static kaleidok.util.DebugManager.verbose;


public class SttManager
{
  private final Kaleidoscope parent;

  private final STT stt;


  SttManager( Kaleidoscope parent )
  {
    this.parent = parent;

    STT.debug = verbose >= 1;
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
    parent.getAudioProcessingManager().getAudioDispatcher()
      .addAudioProcessor(stt.getAudioProcessor());

    initRecorderIcon();
  }


  private boolean initRecorderIcon()
  {
    String strEnabled =
      parent.getParameter(RecorderIcon.class.getCanonicalName() + ".enabled");
    boolean bEnabled = !"forceoff".equals(strEnabled) &&
      (!STT.debug || DefaultValueParser.parseBoolean(strEnabled, true));

    if (bEnabled) {
      RecorderIcon ri = new RecorderIcon(parent, stt);
      ri.x = parent.width - ri.x;
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

      if (topAlternative != null)
      {
        if (verbose >= 1) {
          System.out.format(
            "[%tc] STT returned (confidence=%.1f%%): %s%n",
            System.currentTimeMillis(),
            topAlternative.confidence * 100, topAlternative.transcript);
        }

        if (!isIgnoreTranscriptionResult()) {
          parent.getChromasthetiationService()
            .submit(topAlternative.transcript);
        }
      }
      else if (verbose >= 1)
      {
        System.out.format(
          "[%tc] STT returned no result.%n",
          System.currentTimeMillis());
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
          System.out.println(
            "Notice: Speech transcription results are configured to be ignored.");
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
