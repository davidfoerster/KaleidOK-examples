package kaleidok.javafx.beans.binding;

import javafx.beans.Observable;
import javafx.beans.binding.DoubleBinding;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FunctionDoubleBinding extends DoubleBinding
{
  private final DoubleSupplier valueSupplier;

  private final Observable[] dependencies;


  public FunctionDoubleBinding( DoubleSupplier valueSupplier )
  {
    this(valueSupplier, (Observable[]) null);
  }


  public FunctionDoubleBinding( DoubleSupplier valueSupplier,
    Observable... dependencies )
  {
    this.valueSupplier = Objects.requireNonNull(valueSupplier);
    this.dependencies = dependencies;
    if (dependencies != null)
      bind(dependencies);
  }


  @Override
  protected double computeValue()
  {
    try
    {
      return valueSupplier.getAsDouble();
    }
    catch (RuntimeException ex)
    {
      Logger.getAnonymousLogger().log(Level.WARNING,
        "Exception while evaluating binding", ex);
      return 0;
    }
  }


  @Override
  public void dispose()
  {
    if (dependencies != null)
      unbind(dependencies);
  }
}
