package com.getflourish.stt;

import ddf.minim.AudioInput;
import ddf.minim.AudioRecorder;
import ddf.minim.Minim;
import javaFlacEncoder.FLAC_FileEncoder;
import processing.core.PApplet;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class STT
{
  public static final int LISTENING = 1;
  public static final int RECORDING = 2;
  public static final int TRANSCRIBING = 3;
  public static final int SUCCESS = 4;
  public static final int ERROR = 5;

  private boolean active;
  boolean analyzing;
  private boolean auto;
  private String key;
  String dataPath;
  private boolean debug;
  FLAC_FileEncoder encoder;
  int fileCount;
  String fileName;
  boolean fired;
  AudioInput in;
  int interval;
  private ArrayList<String> languages;
  String lastStatus;
  boolean log;
  private Minim minim;
  PApplet p;
  String path;
  AudioRecorder recorder;
  boolean recording;
  String recordsPath;
  String result;
  String status;
  int lastStatusCode;
  int statusCode;
  private final ArrayList<TranscriptionThread> threads;
  float threshold;
  Timer timer;
  Timer timer2;
  Method transcriptionEvent;
  Method transcriptionEvent2;
  Method transcriptionEvent3;
  private TranscriptionThread transcriptionThread;
  float volume;
  ArrayList<Float> volumes;

  public STT(PApplet p, String key) {
    this(p, key, false);
  }

  public STT(PApplet p, String key, boolean history) {
    this(p, key, history, null);
  }

  public STT(PApplet p, String key, boolean history, Minim minim) {
    active = false;
    auto = false;
    dataPath = "";
    debug = false;
    fileCount = 0;
    fileName = "";
    interval = 500;
    lastStatus = "";
    path = "";
    recording = false;
    recordsPath = "";
    result = "";
    status = "";
    lastStatusCode = -1;
    statusCode = -1;
    threshold = 5;
    this.p = p;
    this.key = key;
    this.log = history;
    threads = new ArrayList<>();
    languages = new ArrayList<>();
    this.minim = (minim == null) ? new Minim(p) : minim;

    encoder = new FLAC_FileEncoder();
    in = this.minim.getLineIn(1);
    recorder = this.minim.createRecorder(in, path + fileName + fileCount + ".wav", true);
    disableAutoRecord();
    listen();
  }

  public AudioInput getLineIn() {
    return in;
  }

  public void addLanguage(String language) {
    languages.add(language);
  }

  public void removeLanguage(String language) {
    languages.remove(language);
  }

  TranscriptionThread addTranscriptionThread(String language, String key)
  {
    transcriptionThread = new TranscriptionThread(language, key);
    transcriptionThread.debug = debug;
    transcriptionThread.start();
    threads.add(transcriptionThread);
    return transcriptionThread;
  }

  private void killTranscriptionThread(int i)
  {
    threads.remove(i).interrupt();
  }

  private void analyzeEnv()
  {
    if (!analyzing) {
      timer2 = new Timer(2000);
      timer2.start();
      analyzing = true;
      volumes = new ArrayList<>();
    }

    if (timer2 != null) {
      float avg;
      if (!timer2.isFinished()) {
        avg = in.mix.level() * 1000;
        volumes.add(avg);
      } else {
        avg = 0;
        float max = 0;

        for (int i = 0; i < volumes.size(); ++i) {
          avg += volumes.get(i);
          if (volumes.get(i) > max) {
            max = volumes.get(i);
          }
        }

        threshold = (float) Math.ceil(max);
        System.out.println(getTime() + " Volume threshold automatically set to " + threshold);
        analyzing = false;
      }
    }

  }

  public void begin()
  {
    if (!active) {
      onBegin();
      active = true;
      auto = false;
    }

  }

  public void disableAutoRecord()
  {
    auto = false;
    disableAutoThreshold();
    status = "STT info: Manual mode enabled. Use begin() / end() to manage recording.";
  }

  public void disableAutoThreshold() {
    analyzing = false;
  }

  public void disableDebug() {
    debug = false;
  }

  public void disableHistory() {
    log = false;
  }

  private void dispatchTranscriptionEvent(String utterance, float confidence, int s)
  {
    if (transcriptionEvent2 != null) {
      try {
        transcriptionEvent2.invoke(p, utterance, confidence, s);
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        ex.printStackTrace();
      } catch (InvocationTargetException ignored) {
      }
    }

  }

  private void dispatchTranscriptionEventWithLanguage(String utterance, float confidence, String language, int s)
  {
    if (transcriptionEvent3 != null) {
      try {
        transcriptionEvent3.invoke(this.p, utterance, confidence, language, s);
      } catch (IllegalArgumentException | IllegalAccessException ex) {
        ex.printStackTrace();
      } catch (InvocationTargetException ignored) {
      }
    }

  }

  public void dispose() {
  }

  public void draw()
  {
    if (auto)
      handleAuto();

    for (int i = 0; i < threads.size(); ++i) {
      transcriptionThread = threads.get(i);
      transcriptionThread.debug = debug;
      if (statusCode != lastStatusCode) {
        this.dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
          transcriptionThread.getConfidence(), statusCode);
        lastStatusCode = statusCode;
      }

      if (transcriptionThread.isAvailable()) {
        if (transcriptionEvent != null) {
          try {
            transcriptionEvent.invoke(this.p, this.transcriptionThread.getUtterance(),
              this.transcriptionThread.getConfidence());
          } catch (IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
          } catch (InvocationTargetException ignored) {
          }
        } else if (transcriptionEvent2 != null) {
          dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
            transcriptionThread.getConfidence(), transcriptionThread.getStatus());
        } else if (transcriptionEvent3 != null) {
          dispatchTranscriptionEventWithLanguage(transcriptionThread.getUtterance(),
            transcriptionThread.getConfidence(), transcriptionThread.getLanguage(),
            transcriptionThread.getStatus());
        }

        status = "Listening";
        statusCode = LISTENING;
        dispatchTranscriptionEvent(transcriptionThread.getUtterance(),
          transcriptionThread.getConfidence(), statusCode);
        lastStatusCode = statusCode;
        killTranscriptionThread(i);
      }

      if (debug && !status.equals(lastStatus)) {
        System.out.println(getTime() + ' ' + status);
        lastStatus = status;
      }
    }

  }

  public void enableAutoRecord()
  {
    auto = true;
    enableAutoThreshold();
    status = "STT info: Automatic mode enabled. Anything louder than threshold will be recorded.";
  }

  public void enableAutoRecord(float threshold)
  {
    auto = true;
    this.threshold = threshold;
    status = "STT info: Automatic mode enabled. Anything louder than " + threshold + " will be recorded.";
  }

  public void enableAutoThreshold()
  {
    analyzing = false;
    analyzeEnv();
  }

  public void enableDebug()
  {
    debug = true;
    for (int i = 0; i < threads.size(); ++i)
      threads.get(i).debug = debug;
  }

  public void enableHistory() {
    log = true;
  }

  public void end()
  {
    if (active) {
      onSpeechFinish();
      active = false;
      auto = false;
    }
  }

  private String getDateTime()
  {
    return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
  }

  public ArrayList<String> getLanguage() {
    return languages;
  }

  public Minim getMinimInstance() {
    return minim;
  }

  public String getStatus() {
    return status;
  }

  public float getThreshold() {
    return threshold;
  }

  private String getTime()
  {
    return new SimpleDateFormat("HH:mm:ss").format(new Date());
  }

  public float getVolume()
  {
    updateVolume();
    return volume;
  }

  private void handleAuto()
  {
    if (analyzing) {
      analyzeEnv();
    }

    updateVolume();
    if (volume > threshold) {
      onSpeech();
    } else if (timer.isFinished() && volume < threshold && recorder.isRecording() && recording) {
      onSpeechFinish();
    } else if (timer.isFinished() && volume < threshold) {
      startListening();
    }
  }

  private void initFileSystem()
  {
    dataPath = p.dataPath("") + '/';
    recordsPath = getDateTime() + '/';
    path = log ? dataPath + recordsPath : dataPath;

    try {
      new File(this.dataPath + "/").mkdir();
      if (log)
        new File(this.path).mkdir();
    } catch (NullPointerException ex) {
      System.err.println("Could not read files in directory: " + path);
    }

    timer = new Timer(interval);
    p.registerMethod("draw", this);
    p.registerMethod("dispose", this);
  }

  private void listen()
  {
    if (languages != null) {
      for (String language: languages)
        addTranscriptionThread(language, key);
    }

    initFileSystem();
    timer.start();

    try {
      transcriptionEvent = p.getClass().getMethod("transcribe", String.class, Float.TYPE);
    } catch (ReflectiveOperationException ignored) {
    }

    try {
      transcriptionEvent2 = p.getClass().getMethod("transcribe", String.class, Float.TYPE, Integer.TYPE);
    } catch (ReflectiveOperationException ignored) {
    }

    try {
      transcriptionEvent3 = p.getClass().getMethod("transcribe", String.class, Float.TYPE, String.class, Integer.TYPE);
    } catch (ReflectiveOperationException ignored) {
    }

    if (transcriptionEvent == null && transcriptionEvent2 == null && transcriptionEvent3 == null) {
      System.err.println(
        "STT info: use transcribe(String word, float confidence, [int status]) in your main sketch to receive transcription events");
    }

  }

  private void onBegin()
  {
    status = "Recording";
    statusCode = RECORDING;
    startListening();
  }

  private void onSpeech()
  {
    status = "Recording";
    statusCode = RECORDING;
    timer.start();
    recording = true;
  }

  public void onSpeechFinish()
  {
    status = "Transcribing";
    statusCode = TRANSCRIBING;
    fired = false;
    recorder.endRecord();
    recorder.save();
    recording = false;
    dispatchTranscriptionEvent("", 0, TRANSCRIBING);
    String flac = path + fileName + fileCount + ".flac";
    encoder.encode(new File(path + fileName + fileCount + ".wav"), new File(flac));

    boolean exists;
    do {
      exists = new File(flac).exists();
    } while (!exists);

    if (exists) {
      transcribe(flac);
    } else {
      statusCode = ERROR;
      System.err.println("Could not transcribe. File was not encoded in time.");
    }

    if (log)
      ++fileCount;
  }

  public void setLanguage(String language) {
    languages.add(language);
  }

  public void setThreshold(float threshold) {
    threshold = threshold;
  }

  private void startListening()
  {
    recorder.endRecord();
    recorder.save();
    recorder = minim.createRecorder(in, path + fileName + fileCount + ".wav", true);
    recorder.beginRecord();
    timer.start();
  }

  public void transcribe(String path)
  {
    for (String language: languages)
      addTranscriptionThread(language, key).startTranscription(path);
  }

  public void transcribeFile(String path)
  {
    status = "Transcribing";
    statusCode = TRANSCRIBING;
    path = this.path + '/' + path;
    String flac = path.substring(0, path.length() - 4) + ".flac";
    encoder.encode(new File(path), new File(flac));

    boolean exists;
    do {
      exists = new File(flac).exists();
    } while (!exists);

    if (exists) {
      transcribe(flac);
    } else {
      statusCode = ERROR;
      System.err.println("Could not transcribe. File was not encoded in time.");
    }

    if (log)
      ++fileCount;
  }

  private void updateVolume() {
    volume = in.mix.level() * 1000;
  }
}
