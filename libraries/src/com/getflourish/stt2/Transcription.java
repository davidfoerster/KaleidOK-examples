package com.getflourish.stt2;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import kaleidok.concurrent.CallbackRunnable;
import kaleidok.http.HttpConnection;
import kaleidok.http.JsonHttpConnection;
import kaleidok.http.responsehandler.JsonResponseHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Transcription extends CallbackRunnable<SttResponse>
{
  private final JsonHttpConnection connection;

  private OutputStream outputStream = null;

  protected Transcription( URL url, String mimeType, float sampleRate )
    throws IOException
  {
    this(JsonHttpConnection.openURL(url));
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type",
      String.format("%s; rate=%.0f;", mimeType, sampleRate));
    connection.setDoOutput(true);
    connection.setChunkedStreamingMode(0);
  }

  protected Transcription( JsonHttpConnection connection )
  {
    this.connection = connection;
  }

  public OutputStream getOutputStream() throws IOException
  {
    if (outputStream == null) {
      outputStream = connection.getOutputStream();
    }
    return outputStream;
  }

  @Override
  public SttResponse call() throws IOException
  {
    try {
      SttResponse response;
      if (!STT.debug) {
        response = parse(connection.getReader());
      } else {
        String strResponse = connection.getBody();
        System.out.println(strResponse);
        response = parse(new StringReader(strResponse));
        logResponse(response);
      }
      return response;
    } catch (IOException ex) {
      if (STT.debug)
        System.err.println("I/O ERROR: Network connection failure");
      throw ex;
    }
  }

  protected SttResponse parse( Reader source ) throws IOException
  {
    try (JsonReader jsonReader = new JsonReader(source)) {
      SttResponse response;
      do {
        response =
          JsonResponseHandler.getDefaultGson().fromJson(jsonReader, SttResponse.class);
      } while (response != null && response.isEmpty());
      return response;
    } catch (JsonSyntaxException ex) {
      throw new IOException(
        "PARSE ERROR: Speech could not be interpreted.", ex);
    } catch (JsonIOException ex) {
      throw new IOException(ex);
    }
  }

  protected void logResponse( SttResponse response )
  {
    if (response != null) {
      SttResponse.Result result = response.result[0];
      assert response.result.length == 1;
      SttResponse.Result.Alternative alternative = result.alternative[0];
      System.out.println(
        "Recognized: " + alternative.transcript +
          " (confidence: " + alternative.confidence + ')');
    } else {
      System.out.println("Speech could not be interpreted! Try to shorten the recording.");
    }
  }

  @Override
  public void dispose()
  {
    if (connection.getState() == HttpConnection.CONNECTED) {
      if (outputStream != null) try {
        outputStream.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      try {
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
          connection.getInputStream().close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      connection.disconnect();
    }
  }
}
