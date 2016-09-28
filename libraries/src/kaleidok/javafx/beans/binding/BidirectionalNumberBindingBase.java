package kaleidok.javafx.beans.binding;

import javafx.beans.WeakListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.lang.ref.WeakReference;
import java.util.Objects;


/*
 * Design and implementation heavily inspired by
 * com.sun.javafx.binding.BidirectionalBinding.TypedNumberBidirectionalBinding
 * of OpenJFX 1.8.
 */
public abstract class BidirectionalNumberBindingBase<T extends Number>
  implements ChangeListener<Number>,WeakListener
{
  protected final WeakReference<Property<T>> p1;

  protected final WeakReference<Property<Number>> p2;

  private boolean updating = false;


  protected BidirectionalNumberBindingBase(
    Property<T> p1, Property<Number> p2 )
  {
    this.p1 = new WeakReference<>(Objects.requireNonNull(p1, "property 1"));
    this.p2 = new WeakReference<>(Objects.requireNonNull(p2, "property 2"));
  }


  @Override
  public boolean wasGarbageCollected()
  {
    return p1.get() == null || p2.get() == null;
  }


  @Override
  public void changed( ObservableValue<? extends Number> sourceProperty,
    Number oldValue, Number newValue )
  {
    if (updating)
      return;

    Property<T> p1 = this.p1.get();
    Property<Number> p2 = this.p2.get();
    if (p1 == null || p2 == null)
    {
      if (p1 != null)
        p1.removeListener(this);
      if (p2 != null)
        p2.removeListener(this);
      return;
    }
    try
    {
      updating = true;
      if (sourceProperty == p1)
      {
        p2.setValue(newValue);
      }
      else
      {
        p1.setValue(convertValue(newValue));
      }
    }
    catch (RuntimeException ex1)
    {
      try
      {
        if (p1 == sourceProperty)
        {
          p1.setValue(convertValue(oldValue));
        }
        else
        {
          p2.setValue(oldValue);
        }
      }
      catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception ex2)
      {
        ex2.addSuppressed(ex1);
        javafx.beans.binding.Bindings.unbindBidirectional(p1, p2);
        throw new RuntimeException(String.format(
          "Bidirectional binding failed together with an attempt to restore " +
            "the source property to the previous value. Removing the " +
            "bidirectional binding from properties %s and %s.",
            p1, p2),
          ex2);
      }
      throw new RuntimeException(
        "Bidirectional binding failed, setting to the previous value", ex1);
    }
    finally
    {
      updating = false;
    }
  }


  protected abstract T convertValue( Number v );
}
