package kaleidok.util;

import java.lang.Math;
import java.util.Arrays;
import java.util.List;


public final class Threads
{
  private Threads() { }


  public static List<Thread> getThreads( ThreadGroup threadGroup, boolean recurse )
  {
    Thread[] threads = new Thread[1 << 4];
    int threadCount;
    while ((threadCount = threadGroup.enumerate(threads, recurse)) >= threads.length) {
      threads = new Thread[
        Math.max(threads.length * 2, threadCount + 1)];
    }
    return Arrays.asList(threads).subList(0, threadCount);
  }
}
