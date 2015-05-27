package kaleidok.examples.sphinx;

import processing.core.PApplet;


public class SphinxSpeechSketch extends PApplet implements Thread.UncaughtExceptionHandler
{
  /**
   * Set to a resource path to transcribe a file; set to <code>null</code> to
   * read from the default microphone line.
   */
  public static final String audioFile =
    //"/edu/cmu/sphinx/demo/aligner/10001-90210-01803.wav"
    //"I like hotdogs.16k.wav"
    null
    ;

  private AbstractSpeechRecognizerThread srt;

  @Override
  public void setup()
  {
    try {
      srt = (audioFile != null) ? new StreamSpeechRecognizerThread(audioFile) : new LiveSpeechRecognizerThread();
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


  public static void main( String... args )
  {
    new SphinxSpeechSketch().runSketch(args);
  }
}
