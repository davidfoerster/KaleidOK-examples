package com.getflourish.stt2;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import kaleidok.util.Timer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class VolumeThresholdTracker implements AudioProcessor
{
  private double threshold;

  private final Timer timer = new Timer();

  private final ArrayList<Double> pastVolumes = new ArrayList<>(); // TODO: use arrays

  private boolean shouldAnalyzeVolumeThreshold = false;

  public void setManualThreshold( double threshold )
  {
    setAutoThreshold(false);
    this.threshold = threshold;
  }

  public void setAutoThreshold( boolean enabled )
  {
    shouldAnalyzeVolumeThreshold = enabled;
  }

  public boolean isThresholdAutomatic()
  {
    return shouldAnalyzeVolumeThreshold;
  }

  public double getThreshold()
  {
    return threshold;
  }

  private void startNewAnalysisWindow()
  {
    pastVolumes.clear();
    timer.reset(2000);
    timer.start();
  }

  public boolean process( AudioEvent audioEvent )
  {
    boolean shouldAnalyzeVolumeThreshold = this.shouldAnalyzeVolumeThreshold;
    if (shouldAnalyzeVolumeThreshold != timer.isStarted()) {
      if (shouldAnalyzeVolumeThreshold) {
        startNewAnalysisWindow();
      } else {
        processingFinished();
      }
    }

    if (timer.isStarted()) {
      if (!timer.isFinished()) {
        pastVolumes.add(audioEvent.getRMS()); // TODO: use common volume level calculator
      } else if (!pastVolumes.isEmpty()) {
        threshold = Math.ceil(Collections.max(pastVolumes));
        System.out.println(getTime() + " Volume threshold automatically set to " + threshold);
        startNewAnalysisWindow();
      }
    }

    return true;
  }

  @Override
  public void processingFinished()
  {
    timer.reset(0);
    pastVolumes.clear();
  }


  private final DateFormat timeFormat =  new SimpleDateFormat("HH:mm:ss");

  private String getTime()
  {
    return timeFormat.format(new Date());
  }
}
