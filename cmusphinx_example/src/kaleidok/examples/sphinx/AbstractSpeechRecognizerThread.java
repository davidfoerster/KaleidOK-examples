package kaleidok.examples.sphinx;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;


abstract class AbstractSpeechRecognizerThread extends Thread
{
  protected String utterance = null;

  private boolean doTranscription = false;


  private static final String modelRoot =
    "resource:/edu/cmu/sphinx/models/en-us/";

  protected static final edu.cmu.sphinx.api.Configuration sphinxConfig =
    new edu.cmu.sphinx.api.Configuration()
    {{
        // Set path to acoustic model.
        setAcousticModelPath(modelRoot + "en-us");
        // Set path to dictionary.
        setDictionaryPath(modelRoot + "cmudict-en-us.dict");
        // Set language model.
        setLanguageModelPath(modelRoot + "en-us.lm.dmp");
      }};


  public AbstractSpeechRecognizerThread( String name )
  {
    super(name);
  }

  public AbstractSpeechRecognizerThread( ThreadGroup group, String name )
  {
    super(group, name);
  }

  @Override
  public void run()
  {
    try {
      for (; ; ) {
        synchronized (this) {
          while (!doTranscription) try {
            wait();
          } catch (InterruptedException e) {
            // go on...
          }
        }
        transcribe();
      }
    } catch (IOException ex) {
      UncaughtExceptionHandler h = getUncaughtExceptionHandler();
      if (h != null) {
        h.uncaughtException(this, ex);
      } else {
        ex.printStackTrace();
      }
    }
  }

  protected abstract void transcribe() throws IOException;

  public String getUtterance()
  {
    return utterance;
  }

  public synchronized void startTranscription()
  {
    doTranscription = true;
    notify();
  }

  public void stopTranscription()
  {
    doTranscription = false;
  }

  protected boolean isDoTranscription()
  {
    return doTranscription;
  }
}
