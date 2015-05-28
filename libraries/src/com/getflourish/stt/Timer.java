package com.getflourish.stt;


public class Timer
{
  long savedTime, totalTime;

  public Timer( int totalTime )
  {
    this.totalTime = totalTime * 1000000L;
  }

  public void start()
  {
    savedTime = System.nanoTime();
  }

  public boolean isFinished()
  {
    return (System.nanoTime() - savedTime) > totalTime;
  }
}
