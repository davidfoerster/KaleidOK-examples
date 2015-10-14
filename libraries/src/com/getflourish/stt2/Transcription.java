package com.getflourish.stt2;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import kaleidok.http.HttpConnection;
import kaleidok.http.JsonHttpConnection;
import kaleidok.http.responsehandler.JsonResponseHandler;
import kaleidok.io.platform.PlatformPaths;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.concurrent.FutureCallback;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import static com.getflourish.stt2.STT.logger;


public class Transcription implements Runnable
{
  private final JsonHttpConnection connection;

  public FutureCallback<SttResponse> callback;

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
      outputStream = new TeeOutputStream(
        connection.getOutputStream(), openLogOutputStream());
    }
    return outputStream;
  }


  private static final Format logFileFormat =
    new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS.'flac'");

  private static final OpenOption[] logOpenoptions =
    new OpenOption[]{
      StandardOpenOption.CREATE_NEW
    };

  private OutputStream openLogOutputStream() throws IOException
  {
    Path path =
      PlatformPaths.getDataDir(this.getClass().getPackage().getName())
        .resolve(logFileFormat.format(System.currentTimeMillis()));
    logger.log(Level.FINE, "Recorded speech written to \"{0}\"", path);
    return new BufferedOutputStream(
      Files.newOutputStream(path, logOpenoptions));
  }


  @Override
  public final void run()
  {
    FutureCallback<SttResponse> callback = this.callback;
    SttResponse result;
    try {
      try {
        result = transcribe();
      } catch (Exception ex) {
        if (callback != null) {
          callback.failed(ex);
        } else {
          Thread current = Thread.currentThread();
          current.getUncaughtExceptionHandler().uncaughtException(current, ex);
        }
        return;
      }
    } finally {
      dispose();
    }

    if (callback != null)
      callback.completed(result);
  }


  public SttResponse transcribe() throws IOException
  {
    try {
      SttResponse response;
      if (!logger.isLoggable(Level.FINEST)) {
        response = parse(connection.getReader());
      } else {
        String strResponse = connection.getBody();
        logger.log(Level.FINEST, strResponse);
        response = parse(new StringReader(strResponse));
        logResponse(response);
      }
      return response;
    } catch (IOException ex) {
      throw new IOException("Network connection failure", ex);
    }
  }


  protected SttResponse parse( Reader source ) throws IOException
  {
    try (JsonReader jsonReader = new JsonReader(source)) {
      jsonReader.setLenient(true);
      SttResponse response;
      do {
        response =
          JsonResponseHandler.getDefaultGson().fromJson(jsonReader, SttResponse.class);
      } while ((response == null || response.isEmpty()) && jsonReader.peek() != JsonToken.END_DOCUMENT);
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
      SttResponse.Result.Alternative alternative = response.getTopAlternative();
      if (alternative != null) {
        logger.log(Level.FINE,
          "Recognized: {0} (confidence: {1,number,percent})",
          new Object[]{alternative.transcript, alternative.confidence});
      }
    }
  }


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
