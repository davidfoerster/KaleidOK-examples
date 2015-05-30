package com.getflourish.stt2.mock;

import com.getflourish.stt2.TranscriptionResultHandler;
import com.getflourish.stt2.TranscriptionThread;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;


public class MockTranscriptionThread extends TranscriptionThread
{
  public MockTranscriptionThread( String accessKey, TranscriptionResultHandler resultHandler )
  {
    super(resultHandler);
    System.out.println("Notice: You set your Google API access key to \"" +
      accessKey + "\"; speech transcription is performed by " +
      this.getClass().getCanonicalName() + '.');
  }

  @Override
  protected String fetchTranscriptionResult( InputStream audioInputStream )
    throws IOException
  {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(
        String.format("%s/%s-%tY%<tm%<td-%<tH:%<tM:%<tS.%<tL.flac",
          System.getProperty("java.io.tmpdir"),
          this.getClass().getSimpleName(), new Date())));
      long bytesTransferred = copyStream(audioInputStream, out);
      if (debug) {
        System.out.println(
          this.getClass().getSimpleName() + " finished reading " +
            bytesTransferred + " bytes.");
      }
      return (bytesTransferred > 86) ? transcriptionResult : null;
    } finally {
      if (out != null)
        out.close();
    }
  }

  public static final String transcriptionResult =
    "{\"result\":[]}\n" +
      "{\"result\":[{" +
      "\"alternative\":[" +
      "{\"transcript\":\"I like hot dogs\",\"confidence\":0.95803052}," +
      "{\"transcript\":\"I like hotdogs\"}," +
      "{\"transcript\":\"I like hot stocks\"}," +
      "{\"transcript\":\"I'll like hotdogs\"}," +
      "{\"transcript\":\"I like a hot stocks\"}" +
      "]," +
      "\"final\":true" +
      "}]," +
      "\"result_index\":0}\n";
}
