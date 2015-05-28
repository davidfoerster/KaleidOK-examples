package com.getflourish.stt;


import com.getflourish.stt.ClientHttpRequest;
import com.getflourish.stt.Response;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TranscriptionThread extends Thread
{
  boolean running = false;
  private float confidence;
  public boolean debug = false;
  private int status;
  boolean available = false;
  private String utterance;
  private String record;
  private final String lang;
  private final String key;

  public TranscriptionThread(String lang, String key)
  {
    super("Speech transcription");
    this.lang = lang;
    this.key = key;
  }

  public void startTranscription(String record)
  {
    this.record = record;
    running = true;
  }

  public void run()
  {
    while (!Thread.currentThread().isInterrupted())
    {
      if (running) {
        transcribe(record);
        running = false;
      } else {
        try {
          sleep(500);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }

  }

  public boolean isRunning() {
    return running;
  }

  public String transcribe(String path)
  {
    available = false;
    File file = new File(path);
    String result = "";

    try {
      ClientHttpRequest r = new ClientHttpRequest("https://www.google.com/speech-api/v2/recognize?output=json&lang=" + this.lang + "&key=" + this.key);
      r.setParameter("file", file);
      InputStream s = r.post();
      result = this.convertStreamToString(s);

      try {
        result = result.substring("{\"result\":[]}".length());
      } catch (Exception var10) {
        System.err.println("Error parsing JSON. Please check your API key.");
      }
    } catch (IOException var11) {
      ;
    }

    String s1 = "";

    try {
      Gson e = new Gson();
      Response transcription = (Response)e.fromJson(result, Response.class);
      if(transcription != null) {
        if(transcription.result[0].status == 0) {
          this.confidence = transcription.result[0].alternative[0].confidence;
          this.status = transcription.result[0].status;
          this.utterance = transcription.result[0].alternative[0].transcript;
          this.available = true;
        } else {
          this.confidence = 0.0F;
          this.status = transcription.result[0].status;
          this.utterance = "";
          this.available = true;
        }

        if(this.debug) {
          switch(this.status) {
          case 0:
            s1 = "Recognized: " + this.utterance + " (confidence: " + this.confidence + ")";
            this.status = 4;
            break;
          case 1:
          case 2:
          case 4:
          default:
            s1 = "Did you say something?";
            this.status = 5;
            break;
          case 3:
            s1 = "We lost some words on the way.";
            this.status = 5;
            break;
          case 5:
            s1 = "Speech could not be interpreted.";
            this.status = 5;
          }
        } else if(this.status == 0) {
          this.status = 4;
        } else {
          this.status = 5;
        }
      } else {
        s1 = "Speech could not be interpreted! Try to shorten the recording.";
        this.status = 5;
      }

      if(this.debug) {
        System.out.println(this.getTime() + " " + s1);
      }
    } catch (JsonSyntaxException var9) {
      s1 = "PARSE ERROR: Speech could not be interpreted.";
      this.status = 5;
    }

    return this.utterance;
  }

  private static String convertStreamToString(InputStream is) throws IOException
  {
    if (is != null) {
      StringWriter writer = new StringWriter();
      char[] buffer = new char[64 << 10];

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        int n;
        while((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } finally {
        is.close();
      }

      return writer.toString();
    } else {
      return "";
    }
  }

  public float getConfidence() {
    return confidence;
  }

  public String getUtterance() {
    return utterance;
  }

  public int getStatus() {
    return status;
  }

  public String getLanguage() {
    return lang;
  }

  public boolean isAvailable() {
    return available;
  }

  private String getTime()
  {
    return new SimpleDateFormat("HH:mm:ss").format(new Date());
  }
}
