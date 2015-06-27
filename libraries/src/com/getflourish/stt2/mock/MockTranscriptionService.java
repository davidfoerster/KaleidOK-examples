package com.getflourish.stt2.mock;

import com.getflourish.stt2.SttResponse;
import com.getflourish.stt2.TranscriptionService;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;


public class MockTranscriptionService extends TranscriptionService
{
  public static final URL MOCK_API_BASE;
  static {
    try {
      MOCK_API_BASE = new URL(
        "http", "localhost", 8081, DEFAULT_API_BASE.getPath());
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }

  private static HttpServer server = null;

  protected static void initServer() throws IOException
  {
    if (server == null) {
      server = HttpServer.create(new InetSocketAddress(MOCK_API_BASE.getPort()), 0);
      server.createContext(MOCK_API_BASE.getPath(), new MockSpeechToTextHandler());
      server.start();
    }
  }

  public MockTranscriptionService( String accessKey, FutureCallback<SttResponse> resultHandler )
  {
    super(MOCK_API_BASE, accessKey, resultHandler);
    try {
      initServer();
    } catch (IOException ex) {
      throw new Error(ex);
    }
    System.out.println("Notice: You set your Google API access key to \"" +
      accessKey + "\"; speech transcription is performed by " +
      this.getClass().getCanonicalName() + '.');
  }
}
