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
      SttResponse.Result result = response.result[0];

      if (verbose >= 1) {
        System.out.println(
          "STT returned: " + result.alternative[0].transcript);
      }

      if (!isIgnoreTranscriptionResult()) {
        parent.getChromasthetiationService()
          .submit(result.alternative[0].transcript);
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
