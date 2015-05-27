package kaleidok.examples.sphinx;

import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.util.Arrays;


public class StreamSpeechRecognizerThread extends AbstractSpeechRecognizerThread
{
  private final AudioInputStream inputStream;

  private final StreamSpeechRecognizer recognizer;

  public StreamSpeechRecognizerThread( String audioFile ) throws IOException, UnsupportedAudioFileException
  {
    this(getAudioInputStream(audioFile));
  }

  private static AudioInputStream getAudioInputStream( String path )
    throws IOException, UnsupportedAudioFileException
  {
    URL url = StreamSpeechRecognizerThread.class.getResource(path);
    if (url != null)
      return AudioSystem.getAudioInputStream(url);

    throw new FileNotFoundException(
      "Error: Couldn't open resource \"" + path + '\"');
  }

  public StreamSpeechRecognizerThread( AudioInputStream inputStream )
    throws IOException, UnsupportedAudioFileException
  {
    super("Speech transcription");

    this.inputStream = inputStream;
    AudioFormat format = inputStream.getFormat();
    if (format.getChannels() != 1 || format.getFrameRate() != sphinxConfig.getSampleRate())
      throw new UnsupportedAudioFileException("The audio format of is not 16 kHz mono");
    if (isRandomAccessStream()) {
      inputStream.mark(
        (int)(inputStream.getFrameLength() * format.getFrameSize()));
    }
    recognizer = new StreamSpeechRecognizer(sphinxConfig);
  }

  protected void transcribe() throws IOException
  {
    System.out.println("Start speech recognition...");
    recognizer.startRecognition(inputStream);

    System.out.println("Transcribe results...");
    StringBuilder utterance = new StringBuilder();
    SpeechResult sr;
    while ((sr = recognizer.getResult()) != null) {
      utterance.append(sr.getHypothesis()).append('\n');
    }

    recognizer.stopRecognition();
    System.out.println("Stopped speech recognition");
    if (isRandomAccessStream())
      inputStream.reset();

    this.utterance = utterance.toString();
    System.out.println("Hypotheses:");
    System.out.println(this.utterance);
  }

  public boolean isRandomAccessStream()
  {
    return inputStream.markSupported() && inputStream.getFrameLength() != AudioSystem.NOT_SPECIFIED;
  }
}
