package kaleidok.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;


public class SerialExecutorService implements Executor, Runnable
{
  private volatile BlockingQueue<Runnable> tasks;

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
      throw new IllegalStateException("shut down");
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
    BlockingQueue<Runnable> tasks = this.tasks;
    if (tasks != null) {
      //noinspection MismatchedQueryAndUpdateOfCollection
      Collection<Runnable> remaining = new ArrayList<>(tasks.size());
      tasks.drainTo(remaining);
      tasks.add(null);
      this.tasks = null;
    }
  }

  public boolean isShutdown()
  {
    return tasks == null;
  }

  public boolean contains( Runnable r )
  {
    BlockingQueue<Runnable> tasks = this.tasks;
    if (tasks != null)
      return tasks.contains(r);

    throw new IllegalStateException("shut down");
  }
}
