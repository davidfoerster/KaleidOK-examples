package com.getflourish.stt2;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;


public class TranscriptionThreadTest
{
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

  private StringReader transcriptionResultReader =
    new StringReader(transcriptionResult) {{
      try {
        mark(transcriptionResult.length());
      } catch (IOException e) {
        throw new Error(e);
      }
    }};

  private TranscriptionThread tt;
  {
    try {
      tt = new TranscriptionThread(new URL("http", "localhost", -1, "/"), "TEST", null);
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }


  @Before
  public void setUp() throws IOException
  {
    transcriptionResultReader.reset();
  }

  private static final Method parseTranscriptionResult;
  static {
    try {
      parseTranscriptionResult =
        TranscriptionThread.class.getDeclaredMethod("parseTranscriptionResult", Reader.class);
      parseTranscriptionResult.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  private Response parseTranscriptionResult( Reader source ) throws IOException
  {
    try {
      return (Response) parseTranscriptionResult.invoke(tt, source);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException)
        throw (IOException) cause;
      throw new Error(cause);
    }
  }

  @Test
  public void testParseTranscriptionResult1() throws IOException
  {
    Response response = parseTranscriptionResult(transcriptionResultReader);
    assertNotNull(response);
    assertEquals(1, response.result.length);

    Response.Result result = response.result[0];
    assertEquals(5, result.alternative.length);

    Response.Result.Alternative alt = result.alternative[0];
    assertEquals("I like hot dogs", alt.transcript);
    assertEquals(0.95803052f, alt.confidence, 1e-9f);

    assertEquals("I like hotdogs", result.alternative[1].transcript);
    assertEquals("I like hot stocks", result.alternative[2].transcript);
    assertEquals("I'll like hotdogs", result.alternative[3].transcript);
    assertEquals("I like a hot stocks", result.alternative[4].transcript);
  }
}
