package kaleidok.kaleidoscope;

import javafx.beans.property.BooleanProperty;
import kaleidok.google.speech.RecorderIcon;
import kaleidok.google.speech.STT;
import kaleidok.google.speech.SttResponse;
import kaleidok.javafx.beans.property.AspectedBooleanProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.util.concurrent.AbstractFutureCallback;
import kaleidok.processing.Plugin;
import processing.event.KeyEvent;

import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import static kaleidok.kaleidoscope.Kaleidoscope.logger;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


public final class SttManager extends Plugin<Kaleidoscope>
  implements PreferenceBean
{
  private final STT stt;

  public final RecorderIcon recorderIcon;

  private final AspectedBooleanProperty enableResponseHandler;


  SttManager( Kaleidoscope sketch )
  {
    super(sketch);

    Map<String, String> params = sketch.getParameterMap();
    stt = new STT(AbstractFutureCallback.getInstance(this::handleSttResponse),
      sketch.parseStringOrFile(
        params.get("com.google.developer.api.key"), '@'))
      {
        @Override
        public Object getParent()
        {
          return SttManager.this;
        }
      };

    sketch.getAudioProcessingManager().getAudioDispatcher()
      .addAudioProcessor(stt.getAudioProcessor());
    recorderIcon = new RecorderIcon(sketch, stt.statusProperty(), 0);

    enableResponseHandler =
      new AspectedBooleanProperty(this, "enable response handler", true);
    enableResponseHandler.addAspect(
      PropertyPreferencesAdapterTag.getInstance());

    getPreferenceAdapters().forEach(PropertyPreferencesAdapter::load);
    initLogfilePathFormat();
  }


  private void initLogfilePathFormat()
  {
    if (isNotEmpty(stt.getLogfilePathFormatString()))
      return;

    String s = p.getParameterMap().get(
      stt.getClass().getName() + ".log.pattern");
    if (isNotEmpty(s))
      stt.setLogfilePathFormatString(s);
  }


  public BooleanProperty enableResponseHandlerProperty()
  {
    return enableResponseHandler;
  }


  private void handleSttResponse( SttResponse response )
  {
    SttResponse.Result.Alternative topAlternative =
      response.getTopAlternative();

    if (topAlternative != null)
    {
      logger.log(Level.INFO,
        "Transcribed with confidence {0,number,percent}: {1}",
        new Object[]{topAlternative.confidence, topAlternative.transcript});

      if (enableResponseHandlerProperty().get())
        p.getChromasthetiationService().submit(topAlternative.transcript);
    }
    else
    {
      logger.info("Transcription returned no result");
    }
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
    return "Speech-to-Text Manager";
  }


  @Override
  public Stream<? extends PropertyPreferencesAdapter<?, ?>>
  getPreferenceAdapters()
  {
    Stream<Stream<? extends PropertyPreferencesAdapter<?,?>>> s = Stream.of(
      stt.getPreferenceAdapters(),
      recorderIcon.getPreferenceAdapters(),
      Stream.of(enableResponseHandler.getAspect(
        PropertyPreferencesAdapterTag.getWritableInstance())));

    return s.flatMap(Function.identity());
  }
}
