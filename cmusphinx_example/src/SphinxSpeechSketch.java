import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import processing.core.PApplet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;


public class SphinxSpeechSketch extends PApplet implements Thread.UncaughtExceptionHandler
{
  /**
   * Set to a resource path to transcribe a file; set to <code>null</code> to
   * read from the default microphone line.
   */
  public static final String audioFile =
    "/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav"
    //"I like hotdogs.16k.wav"
    //null
    ;

  private SpeechRecognizerThread srt;

  @Override
  public void setup()
  {
    try {
      srt = new SpeechRecognizerThread(audioFile);
      srt.setUncaughtExceptionHandler(this);
      srt.start();
    } catch (Exception e) {
      e.printStackTrace();
      exit();
      return;
    }

    size(500, 200);
    noLoop();
  }

  @Override
  public void draw()
  {
    background(255);
    fill(0);

    String utterance = srt.getUtterance();
    if (utterance != null && !utterance.isEmpty())
      text(utterance, 5, 20);
  }

  @Override
  public void keyPressed()
  {
    srt.startTranscription();
  }

  @Override
  public void keyReleased()
  {
    srt.stopTranscription();
  }

  @Override
  public void uncaughtException( Thread thread, Throwable ex )
  {
    ex.printStackTrace();
    assert thread == srt;
    exit();
  }


  private class SpeechRecognizerThread extends Thread
  {
    public final String audioFile;

    private final AbstractSpeechRecognizer recognizer;

    private String utterance = null;

    private boolean stopTranscription = true;

    private static final String modelRoot =
      "resource:/edu/cmu/sphinx/models/en-us/";

    public SpeechRecognizerThread( String audioFile ) throws IOException
    {
      super("Speech transcription");
      this.audioFile = audioFile;

      edu.cmu.sphinx.api.Configuration sphinxConfig =
        new edu.cmu.sphinx.api.Configuration() {{
          // Set path to acoustic model.
          setAcousticModelPath(modelRoot + "en-us");
          // Set path to dictionary.
          setDictionaryPath(modelRoot + "cmudict-en-us.dict");
          // Set language model.
          setLanguageModelPath(modelRoot + "en-us.lm.dmp");
        }};

      recognizer = (this.audioFile != null) ?
        new StreamSpeechRecognizer(sphinxConfig) :
        new LiveSpeechRecognizer(sphinxConfig);
    }

    @Override
    public void run()
    {
      try {
        for (;;) {
          synchronized (this) {
            while (stopTranscription) try {
              wait();
            } catch (InterruptedException e) {
              // go on...
            }
          }
          transcribe();
        }
      } catch (IOException | UnsupportedAudioFileException ex) {
        UncaughtExceptionHandler h = getUncaughtExceptionHandler();
        if (h != null) {
          uncaughtException(this, ex);
        } else {
          ex.printStackTrace();
        }
      }
    }

    private void transcribe() throws IOException, UnsupportedAudioFileException
    {
      stopTranscription = false;
      println("Start speech recognition...");
      if (audioFile != null) {
        URL url = this.getClass().getResource(audioFile);
        if (url != null) {
          AudioInputStream is = AudioSystem.getAudioInputStream(url);
          ((StreamSpeechRecognizer) recognizer).startRecognition(is);
        }
        else {
          throw new FileNotFoundException("Error: Couldn't open resource \"" + audioFile + '\"');
        }
      } else {
        // Start recognition process pruning previously cached data.
        ((LiveSpeechRecognizer) recognizer).startRecognition(true);
      }

      println("Transcribe results...");
      StringBuilder utterance = new StringBuilder();
      SpeechResult sr;
      while ((sr = recognizer.getResult()) != null) {
        utterance.append(sr.getHypothesis()).append('\n');
        if (stopTranscription)
          break;
      }
      stopTranscription = true;

      // Pause recognition process. It can be resumed then with startRecognition(false).
      try {
        recognizer.getClass().getMethod("stopRecognition").invoke(recognizer);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
        // this exception results from a programming error
        throw new Error(ex);
      }
      println("Stopped speech recognition");

      this.utterance = utterance.toString();
      println("Hypotheses:");
      println(this.utterance);

      redraw();
    }

    public String getUtterance()
    {
      return utterance;
    }

    public synchronized void startTranscription()
    {
      stopTranscription = false;
      notify();
    }

    public void stopTranscription()
    {
      if (recognizer instanceof LiveSpeechRecognizer)
        stopTranscription = true;
    }
  }


  public static void main( String... args )
  {
    new SphinxSpeechSketch().runSketch(args);
  }
}
