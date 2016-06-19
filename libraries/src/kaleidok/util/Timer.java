package kaleidok.util;


import java.util.concurrent.TimeUnit;


public class Timer
{
  private long savedTime = -1, totalTime;


  public Timer()
  {
    this(-1, TimeUnit.NANOSECONDS);
  }

  public Timer( long totalTime, TimeUnit unit )
  {
    this.totalTime = unit.toNanos(totalTime);
  }


  public void reset( long totalTime, TimeUnit unit )
  {
    this.totalTime = unit.toNanos(totalTime);
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


  public long getTotalTime()
  {
    return totalTime;
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
