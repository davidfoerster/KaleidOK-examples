package kaleidok.google.speech;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import kaleidok.http.JsonHttpConnection;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.io.platform.PlatformPaths;
import kaleidok.text.IMessageFormat;
import kaleidok.util.Threads;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.http.concurrent.FutureCallback;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import static kaleidok.google.speech.STT.logger;
import static kaleidok.http.HttpConnection.ConnectionState.CONNECTED;


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
      String.format(Locale.ROOT, "%s; rate=%.0f;", mimeType, sampleRate));
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
      OutputStream
        connOs = connection.getOutputStream(),
        copyOs = openLogOutputStream();
      outputStream =
        (copyOs != null) ? new TeeOutputStream(connOs, copyOs) : connOs;
    }
    return outputStream;
  }


  public IMessageFormat logfilePathFormat;


  private static final OpenOption[] logfileOpenOptions = {
      StandardOpenOption.CREATE_NEW
    };

  private OutputStream openLogOutputStream() throws IOException
  {
    if (!logfilePathFormat.isAvailable())
      return null;

    String pathName = logfilePathFormat.format(new Date());
    if (pathName == null)
      return null;

    Path path =
      PlatformPaths.getDataDir(this.getClass().getPackage().getName())
        .resolve(pathName);
    logger.log(Level.FINE, "Recorded speech will be written to \"{0}\"", path);
    return new BufferedOutputStream(
      Files.newOutputStream(path, logfileOpenOptions));
  }


  @Override
  public void run()
  {
    FutureCallback<SttResponse> callback = this.callback;
    SttResponse result;
    try {
      try {
        result = transcribe();
      } finally {
        dispose();
      }
    } catch (IOException | JsonSyntaxException ex) {
      if (callback != null) {
        callback.failed(ex);
      } else {
        Threads.handleUncaught(ex);
      }
      return;
    }

    if (callback != null)
      callback.completed(result);
  }


  public SttResponse transcribe() throws IOException, JsonSyntaxException
  {
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
  }


  protected static SttResponse parse( Reader source )
    throws IOException, JsonSyntaxException
  {
    try (JsonReader jsonReader = new JsonReader(source)) {
      jsonReader.setLenient(true);
      SttResponse response;
      do {
        response =
          TypeAdapterManager.getGson().fromJson(jsonReader, SttResponse.class);
      } while ((response == null || response.isEmpty()) && jsonReader.peek() != JsonToken.END_DOCUMENT);
      return response;
    } catch (JsonIOException ex) {
      throw new IOException(ex);
    }
  }


  protected static void logResponse( SttResponse response )
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


  @OverridingMethodsMustInvokeSuper
  public void dispose()
  {
    if (connection.getState() == CONNECTED) {
      if (outputStream != null) try {
        outputStream.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      try {
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
          //noinspection resource
          connection.getInputStream().close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      connection.disconnect();
    }
  }
}
