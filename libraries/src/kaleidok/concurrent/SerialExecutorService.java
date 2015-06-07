package kaleidok.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;


public class SerialExecutorService implements Executor, Runnable
{
  private BlockingQueue<Runnable> tasks;

  public SerialExecutorService( int capacity )
  {
    this((capacity > 0) ?
      new ArrayBlockingQueue<Runnable>(capacity) :
      new LinkedBlockingDeque<Runnable>());
  }

  public SerialExecutorService( BlockingQueue<Runnable> queue )
  {
    this.tasks = queue;
  }

  @Override
  public synchronized void execute( Runnable r )
  {
    if (r == null)
      throw new NullPointerException();
    if (tasks == null)
      throw new RejectedExecutionException("Service has been shut down");
    tasks.add(r);
  }

  @Override
  public void run()
  {
    BlockingQueue<Runnable> tasks;
    while ((tasks = this.tasks) != null) {
      Runnable r;
      try {
        r = tasks.take();
      } catch (InterruptedException e) {
        continue;
      }
      if (r != null)
        r.run();
    }
  }

  public synchronized void shutdownNow()
  {
    if (tasks != null) {
      Collection<Runnable> remaining = new ArrayList<>(tasks.size());
      tasks.drainTo(remaining);
      tasks.add(null);
      tasks = null;

      for (Runnable t: remaining) {
        if (t instanceof CallbackRunnable)
          ((CallbackRunnable) t).dispose();
      }
    }
  }

  public boolean contains( Runnable r )
  {
    BlockingQueue<Runnable> tasks = this.tasks;
    return tasks != null && tasks.contains(r);
  }
}
