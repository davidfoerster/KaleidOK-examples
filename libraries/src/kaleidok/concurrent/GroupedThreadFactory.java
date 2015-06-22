package kaleidok.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class GroupedThreadFactory implements ThreadFactory
{
  public final ThreadGroup group;

  private final AtomicInteger idCounter = new AtomicInteger(0);


  public GroupedThreadFactory( ThreadGroup group )
  {
    this.group = group;
  }

  public GroupedThreadFactory( String groupName, boolean daemon )
  {
    this(new ThreadGroup(groupName));
    group.setDaemon(daemon);
  }


  @Override
  public Thread newThread( Runnable r )
  {
    Thread thread = new Thread(group, r,
      group.getName() + '-' + idCounter.incrementAndGet());
    thread.setDaemon(group.isDaemon());
    return thread;
  }
}
