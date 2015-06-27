package kaleidok.concurrent;

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
    Thread thread = new Thread(r, name);
    thread.setDaemon(asDaemon);
    return thread;
  }
}
