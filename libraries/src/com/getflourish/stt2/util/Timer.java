package com.getflourish.stt2.util;


public class Timer
{
  long savedTime, totalTime;

  public Timer()
  {
    this(-1);
  }

  public Timer( int totalTime )
  {
    reset(totalTime);
  }

  public void reset( int totalTime )
  {
    this.totalTime = totalTime * 1000000L;
    reset();
  }

  public void reset()
  {
    savedTime = -1;
  }

  public void start()
  {
    savedTime = System.nanoTime();
  }

  public long getRuntime()
  {
    return isStarted() ? System.nanoTime() - savedTime : -1;
  }

  public boolean isStarted()
  {
    return savedTime >= 0;
  }

  public boolean isFinished()
  {
    return getRuntime() > totalTime;
  }
}
