package kaleidok.util;


import java.util.concurrent.TimeUnit;


public class Timer
{
  private long savedTime, totalTime;

  private boolean started = false;


  protected Timer( long totalTime )
  {
    this.totalTime = totalTime;
  }


  public Timer()
  {
    this(-1);
  }


  public Timer( long totalTime, TimeUnit unit )
  {
    this(unit.toNanos(totalTime));
  }


  public void reset( long totalTime, TimeUnit unit )
  {
    this.totalTime = unit.toNanos(totalTime);
    reset();
  }


  public void reset()
  {
    started = false;
  }


  public void start()
  {
    savedTime = System.nanoTime();
    started = true;
  }


  public long getTotalTime()
  {
    return totalTime;
  }


  public long getRuntime()
  {
    if (started)
      return System.nanoTime() - savedTime;

    throw new IllegalStateException("Not running");
  }


  public boolean isStarted()
  {
    return started;
  }


  public boolean isFinished()
  {
    long totalTime = this.totalTime;
    if (totalTime > 0)
      return getRuntime() > totalTime;

    throw new IllegalStateException("No total time set");
  }
}
