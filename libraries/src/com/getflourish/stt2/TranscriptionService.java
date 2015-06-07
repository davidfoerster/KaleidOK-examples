package com.getflourish.stt2;

import kaleidok.concurrent.Callback;
import kaleidok.concurrent.SerialExecutorService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


public class TranscriptionService implements Runnable
{
  private URL apiBase, serviceUrl = null;

  private String accessKey, language;

  public Callback<Response> resultHandler;

  protected final SerialExecutorService executor =
    new SerialExecutorService(3);


  public static final URL DEFAULT_API_BASE;
  static {
    try {
      DEFAULT_API_BASE = new URL("https", "www.google.com", "/speech-api/v2/");
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }


  public TranscriptionService( String accessKey, Callback<Response> resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }

  public TranscriptionService( URL apiBase, String accessKey, Callback<Response> resultHandler )
  {
    this(resultHandler);
    setServiceUrl(apiBase, accessKey, "en");
  }

  protected TranscriptionService( Callback<Response> resultHandler )
  {
    this.resultHandler = resultHandler;
  }


  public URL getApiBase()
  {
    return apiBase;
  }

  public void setApiBase( URL apiBase )
  {
    setServiceUrl(apiBase, accessKey, language);
  }

  public String getAccessKey()
  {
    return accessKey;
  }

  public void setAccessKey( String accessKey )
  {
    setServiceUrl(apiBase, accessKey, language);
  }

  public String getLanguage()
  {
    return language;
  }

  public void setLanguage( String language )
  {
    setServiceUrl(apiBase, accessKey, language);
  }

  protected URL getServiceUrl()
  {
    return serviceUrl;
  }

  protected void setServiceUrl( URL apiBase, String accessKey, String language )
  {
    try {
      this.serviceUrl = new URL(apiBase, "recognize?output=json" +
        "&lang=" + urlEncode(language) +
        "&key=" + urlEncode(accessKey));
      this.apiBase = apiBase;
      this.accessKey = accessKey;
      this.language = language;
    } catch (MalformedURLException ex) {
      throw new Error(ex);
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

  public void add( Transcription task )
  {
    executor.execute(task);
  }

  @Override
  public void run()
  {
    executor.run();
  }

  public void shutdownNow()
  {
    executor.shutdownNow();
  }
}
