package com.getflourish.stt2;

import kaleidok.concurrent.DaemonThreadFactory;
import org.apache.http.concurrent.FutureCallback;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static kaleidok.http.URLEncoding.appendEncoded;


public class TranscriptionService
{
  private URL apiBase;

  private String accessKey;

  private String language = "en";

  public FutureCallback<SttResponse> resultHandler;


  protected final ExecutorService executor =
    new ThreadPoolExecutor(1, 1, 0, TimeUnit.NANOSECONDS,
      new ArrayBlockingQueue<Runnable>(3), executorThreadFactory);

  protected final static ThreadFactory executorThreadFactory =
    new DaemonThreadFactory("Speech transcription", true);


  public static final URL DEFAULT_API_BASE;
  static {
    try {
      DEFAULT_API_BASE = new URL("https", "www.google.com", "/speech-api/v2/");
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }


  public TranscriptionService( String accessKey, FutureCallback<SttResponse> resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }

  public TranscriptionService( URL apiBase, String accessKey, FutureCallback<SttResponse> resultHandler )
  {
    this(resultHandler);
    this.apiBase = apiBase;
    this.accessKey = accessKey;
  }

  protected TranscriptionService( FutureCallback<SttResponse> resultHandler )
  {
    this.resultHandler = resultHandler;
  }


  public URL getApiBase()
  {
    return apiBase;
  }

  public void setApiBase( URL apiBase )
  {
    this.apiBase = apiBase;
    serviceUrl = null;
  }


  public String getAccessKey()
  {
    return accessKey;
  }

  public void setAccessKey( String accessKey )
  {
    this.accessKey = accessKey;
    serviceUrl = null;
  }


  public String getLanguage()
  {
    return language;
  }

  public void setLanguage( String language )
  {
    this.language = language;
    serviceUrl = null;
  }


  protected URL getServiceUrl()
  {
    if (serviceUrl == null) {
      StringBuilder urlSpec = new StringBuilder(
        URL_SPEC_PREFIX.length() + language.length() +
          URL_SPEC_KEY_BIT.length() + accessKey.length());
      appendEncoded(language, urlSpec.append(URL_SPEC_PREFIX));
      appendEncoded(accessKey, urlSpec.append(URL_SPEC_KEY_BIT));

      try {
        serviceUrl = new URL(apiBase, urlSpec.toString());
      } catch (MalformedURLException ex) {
        throw new AssertionError(ex);
      }
    }
    return serviceUrl;
  }

  private URL serviceUrl = null;

  private static final String
    URL_SPEC_PREFIX = "recognize?output=json&lang=",
    URL_SPEC_KEY_BIT = "&key=";


  public void execute( Transcription task )
  {
    executor.execute(task);
  }

  public void shutdownNow()
  {
    executor.shutdownNow();
  }


  boolean isInQueue( Transcription task )
  {
    return executor instanceof ThreadPoolExecutor &&
      ((ThreadPoolExecutor) executor).getQueue().contains(task);
  }
}
