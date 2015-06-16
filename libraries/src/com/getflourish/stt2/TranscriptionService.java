package com.getflourish.stt2;

import kaleidok.concurrent.Callback;
import kaleidok.concurrent.SerialExecutorService;

import java.net.MalformedURLException;
import java.net.URL;

import static kaleidok.http.URLEncoding.appendEncoded;


public class TranscriptionService implements Runnable
{
  private URL apiBase, serviceUrl = null;

  private String accessKey, language;

  public Callback<SttResponse> resultHandler;

  protected final SerialExecutorService executor =
    new SerialExecutorService(3);


  public static final URL DEFAULT_API_BASE;
  static {
    try {
      DEFAULT_API_BASE = new URL("https", "www.google.com", "/speech-api/v2/");
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }


  public TranscriptionService( String accessKey, Callback<SttResponse> resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }

  public TranscriptionService( URL apiBase, String accessKey, Callback<SttResponse> resultHandler )
  {
    this(resultHandler);
    setServiceUrl(apiBase, accessKey, "en");
  }

  protected TranscriptionService( Callback<SttResponse> resultHandler )
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
    StringBuilder urlSpec = new StringBuilder(
      URL_SPEC_PREFIX.length() + language.length() +
      URL_SPEC_KEY_BIT.length() + accessKey.length());
    appendEncoded(language, urlSpec.append(URL_SPEC_PREFIX));
    appendEncoded(language, urlSpec.append(URL_SPEC_KEY_BIT));

    try {
      this.serviceUrl = new URL(apiBase, urlSpec.toString());
      this.apiBase = apiBase;
      this.accessKey = accessKey;
      this.language = language;
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }

  private static final String
    URL_SPEC_PREFIX = "recognize?output=json&lang=",
    URL_SPEC_KEY_BIT = "&key=";

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
