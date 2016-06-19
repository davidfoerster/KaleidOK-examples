package kaleidok.concurrent;

import java.util.concurrent.Executor;


/**
 * This {@code Executor} implementation executes submitted tasks synchronously
 * upon submission in the submitting thread.
 * <p>
 * A possible use case is testing and debugging of task or event driven code.
 */
public class SynchronousExecutor implements Executor
{
  public static final SynchronousExecutor INSTANCE =
    new SynchronousExecutor();


  protected SynchronousExecutor() { }


  @Override
  public void execute( Runnable command )
  {
    command.run();
  }
}
