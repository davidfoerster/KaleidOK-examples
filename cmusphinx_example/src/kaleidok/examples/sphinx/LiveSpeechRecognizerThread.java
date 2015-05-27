package kaleidok.examples.sphinx;

import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.Arrays;


public class LiveSpeechRecognizerThread extends AbstractSpeechRecognizerThread
{
  private final TargetDataLine line;

  private byte[] audioBuf = null;

  private ByteArrayInputStream inputStream = null;

  private final StreamSpeechRecognizer recognizer;


  private static final String modelRoot =
    "resource:/edu/cmu/sphinx/models/en-us/";

  private static final edu.cmu.sphinx.api.Configuration sphinxConfig =
    new edu.cmu.sphinx.api.Configuration()
    {{
        // Set path to acoustic model.
        setAcousticModelPath(modelRoot + "en-us");
        // Set path to dictionary.
        setDictionaryPath(modelRoot + "cmudict-en-us.dict");
        // Set language model.
        setLanguageModelPath(modelRoot + "en-us.lm.dmp");
      }};

  public LiveSpeechRecognizerThread() throws IOException, LineUnavailableException
  {
    this(1 << 10);
  }

  public LiveSpeechRecognizerThread( int bufferSize ) throws IOException, LineUnavailableException
  {
    super("Live speech transcription");

    AudioFormat format =
      new AudioFormat(sphinxConfig.getSampleRate(), 16, 1, true, false);
    line = AudioSystem.getTargetDataLine(format);
    line.open(format, bufferSize);

    recognizer = new StreamSpeechRecognizer(sphinxConfig);
  }

  protected void transcribe() throws IOException
  {
    System.out.println("Start speech recognition...");
    if (readIntoBuffer(true) < 0) {
      System.out.println("No (more) audio data left to read.");
      stopTranscription();
      return;
    }
    recognizer.startRecognition(inputStream);

    System.out.println("Transcribe results...");
    StringBuilder utterance = new StringBuilder();
    SpeechResult sr;
    while ((sr = recognizer.getResult()) != null) {
      utterance.append(sr.getHypothesis()).append('\n');
    }

    recognizer.stopRecognition();
    System.out.println("Stopped speech recognition");

    this.utterance = utterance.toString();
    System.out.println("Hypotheses:");
    System.out.println(this.utterance);
  }

  private int readIntoBuffer( boolean drain ) throws IOException
  {
    final TargetDataLine line = this.line;
    final int chunkSize = line.getBufferSize();
    byte[] buf = this.audioBuf;
    if (buf == null || buf.length < chunkSize)
      buf = new byte[1 << 20];

    line.start();

    int i = 0, rv = 0;
    while (rv >= 0 && isDoTranscription())
    {
      i += rv;
      if (buf.length < (long) i + chunkSize)
      {
        if (buf.length > Integer.MAX_VALUE / 2)
          throw new BufferOverflowException();
        buf = Arrays.copyOf(buf, buf.length * 2);
      }
      rv = line.read(buf, i, chunkSize);
    }

    line.stop();
    this.audioBuf = buf;
    if (inputStream != null) {
      inputStream.reset(buf, 0, i);
    } else {
      inputStream = new ByteArrayInputStream(buf, 0, i);
    }

    return (i != 0) ? i : rv;
  }
}
