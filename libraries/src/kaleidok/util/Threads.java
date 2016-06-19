package kaleidok.util;

import java.lang.Math;


public final class Threads
{
  private Threads() { }


  public static Thread[] getThreads( ThreadGroup threadGroup, boolean recurse )
  {
    Thread[] threads = new Thread[8];
    int threadCount;
    while ((threadCount = threadGroup.enumerate(threads, recurse)) >= threads.length) {
      assert threads.length <= Integer.MAX_VALUE / 2 && threadCount < Integer.MAX_VALUE;
      threads = new Thread[
        Math.max(threads.length * 2, threadCount + 1)];
    }
    return threads;
  }


  public static void handleUncaught( Throwable throwable )
  {
    Thread thread = Thread.currentThread();
    thread.getUncaughtExceptionHandler().uncaughtException(thread, throwable);
  }
}
