import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import processing.core.PApplet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;


public class SphinxSpeechSketch extends PApplet
{
  public static final String audioFile =
    "/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav"
    //"I like hotdogs.16k.wav"
    ;

  private static final String modelRoot =
    "resource:/edu/cmu/sphinx/models/en-us/";

  private static final edu.cmu.sphinx.api.Configuration sphinxConfig =
    new edu.cmu.sphinx.api.Configuration() {{
      // Set path to acoustic model.
      setAcousticModelPath(modelRoot + "en-us");
      // Set path to dictionary.
      setDictionaryPath(modelRoot + "cmudict-en-us.dict");
      // Set language model.
      setLanguageModelPath(modelRoot + "en-us.lm.dmp");
    }};

  private StreamSpeechRecognizer recognizer;

  private String utterance = null;

  @Override
  public void setup()
  {
    try {
      recognizer = new StreamSpeechRecognizer(sphinxConfig);
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

    if (utterance != null && !utterance.isEmpty())
      text(utterance, 5, 20);
  }

  @Override
  public void keyPressed()
  {
    URL url = this.getClass().getResource(audioFile);
    if (url != null) try {
      AudioInputStream is = AudioSystem.getAudioInputStream(url);
      println("Starting speech recognition...");
      // Start recognition process pruning previously cached data.
      recognizer.startRecognition(is);
      stopSpeechRecognition();
    } catch (IOException | UnsupportedAudioFileException ex) {
      ex.printStackTrace();
    } else {
      println("Error: Couldn't open resource \"" + audioFile + '\"');
    }
  }

  @Override
  public void keyReleased()
  {
    //stopSpeechRecognition();
  }

  private void stopSpeechRecognition()
  {
    StringBuilder utterance = new StringBuilder();
    SpeechResult sr;
    while ((sr = recognizer.getResult()) != null) {
      utterance.append(sr.getHypothesis()).append('\n');
    }
    this.utterance = utterance.toString();

    println("Hypotheses:");
    println(this.utterance);

    // Pause recognition process. It can be resumed then with startRecognition(false).
    recognizer.stopRecognition();
    println("Stopped speech recognition");

    redraw();
  }

  public static void main( String... args )
  {
    new SphinxSpeechSketch().runSketch(args);
  }
}
