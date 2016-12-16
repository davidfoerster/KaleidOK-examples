package kaleidok.javafx.stage;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import javafx.stage.Window;


public class StageOwnerInitializer implements InvalidationListener
{
  public static void apply( ObservableValue<? extends Window> owner,
    ObservableValue<? extends Stage> owned )
  {
    StageOwnerInitializer soi = new StageOwnerInitializer(owner, owned);
    owner.addListener(soi);
    owned.addListener(soi);
  }


  private final ObservableValue<? extends Window> owner;

  private final ObservableValue<? extends Stage> owned;

  private boolean isOwnerSet = false;


  protected StageOwnerInitializer( ObservableValue<? extends Window> owner,
    ObservableValue<? extends Stage> owned )
  {
    this.owner = owner;
    this.owned = owned;
  }


  @Override
  public void invalidated( Observable observable )
  {
    Window owner = this.owner.getValue();
    Stage owned = this.owned.getValue();
    if (owner != null && owned != null)
    {
      this.owner.removeListener(this);
      this.owned.removeListener(this);

      synchronized (this)
      {
        if (!isOwnerSet)
        {
          owned.initOwner(owner);
          isOwnerSet = true;
        }
      }
    }
  }
}
