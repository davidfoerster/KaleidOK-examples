package kaleidok.util;

import java.lang.Math;


public final class Threads
{
  private Threads() { }


  public static Thread[] getThreads( ThreadGroup threadGroup, boolean recurse )
  {
    Thread[] threads = new Thread[8];
    int threadCount;
    while ((threadCount = threadGroup.enumerate(threads, recurse)) >= threads.length)
    {
      threads = new Thread[Math.max(
        Math.multiplyExact(threads.length, 2),
        Math.incrementExact(threadCount))];
    }
    return threads;
  }


  public static void handleUncaught( Throwable throwable )
  {
    Thread thread = Thread.currentThread();
    thread.getUncaughtExceptionHandler().uncaughtException(thread, throwable);
  }
}
