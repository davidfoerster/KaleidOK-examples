package com.getflourish.stt2;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class TranscriptionThread extends Thread
{
  public String language = null;

  protected URL apiBase;

  protected String urlStub;

  protected BlockingQueue<TranscriptionTask> taskQueue =
    new ArrayBlockingQueue<>(3, false);

  protected TranscriptionResultHandler resultHandler;

  public boolean debug = false;

  protected final Gson gson = new Gson();

  public static final URL DEFAULT_API_BASE;
  static {
    try {
      DEFAULT_API_BASE = new URL("https", "www.google.com", "/speech-api/v2/");
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }

  public TranscriptionThread( String accessKey, TranscriptionResultHandler resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }

  public TranscriptionThread( URL apiBase, String accessKey, TranscriptionResultHandler resultHandler )
  {
    this(resultHandler);
    setUrlBase(apiBase, accessKey);
  }

  protected TranscriptionThread( TranscriptionResultHandler resultHandler )
  {
    super("Speech transcription");
    this.resultHandler = resultHandler;
  }

  public void setUrlBase( URL apiBase, String accessKey )
  {
    if (apiBase.getProtocol() == null || !apiBase.getProtocol().startsWith("http"))
      throw new IllegalArgumentException("Unsupported protocol: " + apiBase.getProtocol());
    if (apiBase.getHost() == null)
      throw new IllegalArgumentException("No host specified");
    if (apiBase.getQuery() != null || apiBase.getRef() != null)
      throw new IllegalArgumentException("URL base must not contain a query or a fragment part");

    this.apiBase = apiBase;
    urlStub = "recognize?output=json&key=" + urlEncode(accessKey);
  }

  public TranscriptionTask createTask( String mimeType, float sampleRate )
    throws IOException
  {
    return new TranscriptionTask(this, mimeType, sampleRate);
  }

  @Override
  public void run()
  {
    try {
      TranscriptionTask task;
      while (resultHandler != null) {
        task = null;
        do {
          try {
            task = taskQueue.take();
          } catch (InterruptedException e) {
            // go on...
          }
        } while (task == null);
        task.transcribe();
      }
    } catch (IOException | JsonSyntaxException ex) {
      UncaughtExceptionHandler h = getUncaughtExceptionHandler();
      if (h != null) {
        h.uncaughtException(this, ex);
      } else {
        ex.printStackTrace();
      }
    }
  }

  protected static String urlEncode( String s )
  {
    try {
      return URLEncoder.encode(s, "US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }
}
