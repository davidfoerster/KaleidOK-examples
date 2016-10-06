package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.ReadOnlyProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.ScrollEvent;
import kaleidok.javafx.beans.property.BoundedValue;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;


public abstract class SpinnerItemProvider<T extends Number>
  extends FilteredTreeItemProvider<Number, Spinner<T>>
{
  @Override
  protected EditorNodeInfo<Spinner<T>, Number> callTypeChecked(
    DynamicEditableTreeItem<Number, Spinner<T>> item )
  {
    SpinnerValueFactory<T> svf = getValueFactory(item.getValue());
    Spinner<T> spinner = makeSpinner(svf);
    //noinspection unchecked
    return EditorNodeInfo.of(
      spinner, (ReadOnlyProperty<Number>) svf.valueProperty(),
      spinner.getEditor().textProperty());
  }


  protected Spinner<T> makeSpinner( SpinnerValueFactory<T> valueFactory )
  {
    Spinner<T> spinner = new Spinner<>(valueFactory);
    spinner.setEditable(true);
    spinner.setOnScroll(scrollEventEventHandler);
    return spinner;
  }


  private final EventHandler<ScrollEvent> scrollEventEventHandler =
    (ev) -> {
      int exponent = (ev.isShiftDown() ? 1 : 0) | (ev.isControlDown() ? 2 : 0);
      ((Spinner<?>) ev.getSource()).increment(
        (int) (ev.getDeltaY() / ev.getMultiplierY() * Math.pow(10, exponent)));
      ev.consume();
    };


  protected abstract SpinnerValueFactory<T> getValueFactory( ReadOnlyProperty<Number> property );


  public static class BoundedValueSpinnerItemProvider<T extends Number>
    extends SpinnerItemProvider<T>
  {
    @Override
    public boolean isApplicable( DynamicEditableTreeItem<Number, Spinner<T>> item )
    {
      return item.getValue() instanceof BoundedValue;
    }


    @Override
    protected SpinnerValueFactory<T> getValueFactory(
      ReadOnlyProperty<Number> property )
    {
      //noinspection unchecked
      return ((BoundedValue<T, ? extends SpinnerValueFactory<T>>) property).getBounds();
    }
  }
}
