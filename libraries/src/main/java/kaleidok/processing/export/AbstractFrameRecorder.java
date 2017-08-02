package kaleidok.processing.export;

import kaleidok.processing.Plugin;
import processing.core.PApplet;

import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractFrameRecorder extends Plugin<PApplet>
{

  private final AtomicReference<State> state =
    new AtomicReference<>(State.OFF);


  protected AbstractFrameRecorder( PApplet sketch )
  {
    super(sketch);
  }


  public abstract boolean isReady();


  @SuppressWarnings("UnusedReturnValue")
  public boolean schedule()
  {
    return schedule(true);
  }

  public boolean schedule( boolean enable )
  {
    State expectedCurrentState, targetState;
    if (enable)
    {
      expectedCurrentState = State.OFF;
      targetState = State.SCHEDULED;
    }
    else
    {
      expectedCurrentState = State.SCHEDULED;
      targetState = State.OFF;
    }
    return state.compareAndSet(expectedCurrentState, targetState);
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


  @SuppressWarnings("EmptyMethod")
  protected void doBeginRecord() { }


  @Override
  public void draw()
  {
    if (state.compareAndSet(State.RECORDING, State.OFF))
    {
      doEndRecord();
    }
  }

  protected void doEndRecord() { }


  protected enum State {
    OFF,
    SCHEDULED,
    RECORDING
  }
}
