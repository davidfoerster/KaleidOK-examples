package com.getflourish.stt2;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import kaleidok.http.HttpConnection;
import kaleidok.http.JsonHttpConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class TranscriptionTask
{
  private final TranscriptionThread tt;

  private final JsonHttpConnection connection;

  private OutputStream outputStream = null;

  private boolean scheduled = false;

  protected TranscriptionTask( TranscriptionThread tt, String mimeType, float sampleRate )
    throws IOException
  {
    this.tt = tt;
    URL url;
    try {
      url = new URL(tt.apiBase, tt.urlStub + "&lang=" +
        TranscriptionThread.urlEncode(tt.language));
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
    connection = JsonHttpConnection.openURL(url);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type",
      String.format("%s; rate=%.0f;", mimeType, sampleRate));
    connection.setDoOutput(true);
    connection.setChunkedStreamingMode(0);
  }

  public OutputStream getOutputStream() throws IOException
  {
    if (outputStream == null) {
      outputStream = connection.getOutputStream();
    }
    return outputStream;
  }

  public void schedule()
  {
    if (scheduled)
      throw new IllegalStateException("already scheduled");

    tt.taskQueue.add(this);
  }

  public boolean isScheduled()
  {
    return scheduled;
  }

  protected void transcribe()
    throws IOException, JsonSyntaxException
  {
    Response response;
    try {
      response = parseTranscriptionResult(connection.getReader());
    } catch (IOException ex) {
      if (tt.debug) {
        System.err.println("I/O ERROR: Network connection failure");
      }
      throw ex;
    } finally {
      finalizeTask();
    }
    handleTranscriptionResponse(response);
  }

  protected Response parseTranscriptionResult( Reader source ) throws IOException
  {
    JsonReader jsonReader = new JsonReader(source);
    jsonReader.setLenient(true);
    try {
      Response response;
      do {
        response = tt.gson.fromJson(jsonReader, Response.class);
      } while (response != null && (response.result == null || response.result.length == 0));
      return response;
    } catch (JsonSyntaxException ex) {
      if (tt.debug) {
        System.out.println("PARSE ERROR: Speech could not be interpreted.");
      }
      throw ex;
    } catch (JsonIOException ex) {
      throw new IOException(ex);
    } finally {
      jsonReader.close();
    }
  }

  protected void handleTranscriptionResponse( Response response )
  {
    if (response != null && response.result != null && response.result.length != 0) {
      Response.Result result = response.result[0];
      assert response.result.length == 1;
      if (tt.debug) {
        Response.Result.Alternative alternative = result.alternative[0];
        System.out.println(
          "Recognized: " + alternative.transcript +
            " (confidence: " + alternative.confidence + ')');
      }
      if (result != null) {
        tt.resultHandler.handleTranscriptionResult(result);
      } else if (tt.debug) {
        System.out.println("Speech could not be interpreted! Try to shorten the recording.");
      }
    }
  }

  public void finalizeTask()
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
