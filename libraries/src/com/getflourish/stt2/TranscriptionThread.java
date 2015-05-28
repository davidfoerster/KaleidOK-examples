package com.getflourish.stt2;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import kaleidok.http.JsonHttpConnection;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


public class TranscriptionThread extends Thread
{
  public String language = null;

  private URL apiBase;

  private String urlStub;

  // TODO: Put these 3 into a separate object and use an interface to attach it
  private InputStream uploadInputStream;

  private String uploadMimeType;

  private float uploadSampleRate;

  private TranscriptionResultHandler resultHandler;

  public boolean debug = false;

  public static final URL DEFAULT_API_BASE;
  static {
    try {
      DEFAULT_API_BASE = new URL("https", "www.google.com", "/speech-api/v2");
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }


  TranscriptionThread( String accessKey, TranscriptionResultHandler resultHandler )
  {
    this(DEFAULT_API_BASE, accessKey, resultHandler);
  }

  TranscriptionThread( URL apiBase, String accessKey, TranscriptionResultHandler resultHandler )
  {
    this(resultHandler);
    setUrlBase(apiBase, accessKey);
  }

  private TranscriptionThread( TranscriptionResultHandler resultHandler )
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
      throw new IllegalArgumentException("URL base must nut contain a query or a fragment part");

    urlStub = "recognize?output=json&key=" + urlEncode(accessKey);
  }

  @Override
  public void run()
  {
    try {
      InputStream uploadInputStream = null;
      while (resultHandler != null) {
        try {
          synchronized (this) {
            while (this.uploadInputStream == null) {
              try {
                wait();
              } catch (InterruptedException ex) {
                // go on...
              }
            }
            uploadInputStream = this.uploadInputStream;
            this.uploadInputStream = null;
          }
          transcribe(uploadInputStream);
        } finally {
          if (uploadInputStream != null)
            uploadInputStream.close();
        }
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

  private void transcribe( InputStream audioInputStream )
    throws IOException, JsonSyntaxException
  {
    try {
      String resultBody = fetchTranscriptionResult(audioInputStream);
      Response response = (resultBody != null) ?
        parseTranscriptionResult(new StringReader(resultBody)) :
        null;
      handleTranscriptionResponse(response);
    } catch (IOException ex) {
      if (debug) {
        System.out.println("I/O ERROR: Network connection failure");
      }
      throw ex;
    }
  }

  private String fetchTranscriptionResult( InputStream audioInputStream )
    throws IOException
  {
    URL url;
    try {
      url = new URL(apiBase, urlStub + "&language=" + urlEncode(language));
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestProperty("Content-Type",
      String.format("%s; rate=%.0f;", uploadMimeType, uploadSampleRate));
    JsonHttpConnection jsonCon = new JsonHttpConnection(con);
    jsonCon.connect();
    copyStream(audioInputStream, con.getOutputStream());
    return jsonCon.getBody();
  }

  private Response parseTranscriptionResult( Reader source ) throws IOException
  {
    Gson gson = new Gson(); // TODO: re-use Gson object
    JsonReader jsonReader = new JsonReader(source);
    jsonReader.setLenient(true);
    try {
      Response response;
      do {
        response = gson.fromJson(jsonReader, Response.class);
      } while (response != null && (response.result == null || response.result.length == 0));
      return response;
    } catch (JsonSyntaxException ex) {
      if (debug) {
        System.out.println("PARSE ERROR: Speech could not be interpreted.");
      }
      throw ex;
    } catch (JsonIOException ex) {
      throw new IOException(ex);
    } finally {
      jsonReader.close();
    }
  }

  private void handleTranscriptionResponse( Response response )
  {
    if (response != null && response.result != null && response.result.length != 0) {
      Response.Result result = response.result[0];
      assert response.result.length == 1;
      if (debug) {
        Response.Result.Alternative alternative = result.alternative[0];
        System.out.println(
          "Recognized: " + alternative.transcript +
            " (confidence: " + alternative.confidence + ')');
      }
      if (result != null) {
        resultHandler.handleTranscriptionResult(result);
      } else if (debug) {
        System.out.println("Speech could not be interpreted! Try to shorten the recording.");
      }
    }
  }

  public synchronized void attachInput( InputStream in, String mimeType, float sampleRate )
  {
    if (uploadInputStream != null)
      throw new IllegalStateException("There's another stream to transcribe already");

    uploadInputStream = in;
    uploadMimeType = mimeType;
    uploadSampleRate = sampleRate;

    notify();
  }

  private static void copyStream( InputStream input, OutputStream output )
    throws IOException
  {
    byte[] buffer = new byte[64 << 10]; // TODO: re-use buffer
    int bytesRead;
    while ((bytesRead = input.read(buffer)) >= 0)
      output.write(buffer, 0, bytesRead);
  }

  private static String urlEncode( String s )
  {
    try {
      return URLEncoder.encode(s, "US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }
}
