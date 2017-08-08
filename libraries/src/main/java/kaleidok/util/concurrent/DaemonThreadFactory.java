package kaleidok.util.concurrent;

import java.util.concurrent.ThreadFactory;


public class DaemonThreadFactory implements ThreadFactory
{
  public String name;

  public boolean asDaemon;


  public DaemonThreadFactory( String name, boolean asDaemon )
  {
    this.name = name;
    this.asDaemon = asDaemon;
  }


  @Override
  public Thread newThread( Runnable r )
  {
    String name = this.name;
    Thread thread = (name != null) ? new Thread(r, name) : new Thread(r);
    thread.setDaemon(asDaemon);
    return thread;
  }
}
