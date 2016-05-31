package kaleidok.processing.export;

import kaleidok.processing.Plugin;
import processing.core.PApplet;

import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractFrameRecorder extends Plugin<PApplet>
{

  private AtomicReference<State> state = new AtomicReference<>(State.OFF);


  public AbstractFrameRecorder( PApplet sketch )
  {
    super(sketch);
  }


  abstract public boolean isReady();


  public void schedule()
  {
    schedule(true);
  }

  public void schedule( boolean enable )
  {
    State
      expectedCurrentState = enable ? State.OFF : State.RECORDING,
      targetState = enable ? State.SCHEDULED : State.OFF;
    state.compareAndSet(expectedCurrentState, targetState);
  }


  public boolean isScheduled()
  {
    return state.get() == State.SCHEDULED;
  }


  @Override
  public void pre()
  {
    State newState = isReady() ? State.RECORDING : State.OFF;
    if (state.compareAndSet(State.SCHEDULED, newState) &&
      newState == State.RECORDING)
    {
      doBeginRecord();
    }
  }

  protected abstract void doBeginRecord();


  @Override
  public void draw()
  {
    if (state.compareAndSet(State.RECORDING, State.OFF))
    {
      doEndRecord();
    }
  }

  protected abstract void doEndRecord();


  protected enum State {
    OFF,
    SCHEDULED,
    RECORDING
  }
}
