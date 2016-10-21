package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedProperty;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedValueTag;
import kaleidok.javafx.event.EventUtils;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;
import kaleidok.util.Math;

import java.util.Objects;


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
    return EditorNodeInfo.of(spinner, null,
      (StringConverter<Number>) svf.getConverter());
  }


  protected Spinner<T> makeSpinner( SpinnerValueFactory<T> valueFactory )
  {
    Spinner<T> spinner = new Spinner<>(valueFactory);
    spinner.setEditable(true);
    spinner.setOnScroll(SpinnerItemProvider::handleScrollEvent);
    EventUtils.chain(spinner.getEditor().onActionProperty(),
      SpinnerItemProvider::handleActionEvent);
    return spinner;
  }


  protected abstract SpinnerValueFactory<T> getValueFactory( ReadOnlyProperty<Number> property );


  public static class BoundedValueSpinnerItemProvider<T extends Number>
    extends SpinnerItemProvider<T>
  {
    @Override
    public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
    {
      ReadOnlyProperty<?> value = item.getValue();
      //noinspection OverlyStrongTypeCast,unchecked
      return
        value instanceof ObservableNumberValue &&
        value instanceof AspectedProperty &&
          ((AspectedProperty<Number>) value)
            .getAspect(BoundedValueTag.getInstance()) != null;
    }


    @Override
    protected SpinnerValueFactory<T> getValueFactory(
      ReadOnlyProperty<Number> property )
    {
      //noinspection OverlyStrongTypeCast
      return ((AspectedProperty<Number>) property).getAspect(BoundedValueTag.getInstance());
    }
  }


  private static void handleScrollEvent( ScrollEvent ev )
  {
    int exponent = ev.isShiftDown() ? 1 : 0;
    if (ev.isControlDown())
      exponent += 2;

    Spinner<?> spinner = (Spinner<?>) ev.getSource();
    spinner.increment(
      (int)(ev.getDeltaY() / ev.getMultiplierY() * Math.pow10(exponent)));

    ev.consume();
  }


  private static <T> void handleActionEvent( ActionEvent ev )
  {
    @SuppressWarnings("unchecked")
    Spinner<T> spinner = (Spinner<T>) ((Node) ev.getSource()).getParent();
    T newValue = spinner.getValue();

    @SuppressWarnings("unchecked")
    Cell<T> cell = (Cell<T>) spinner.getParent();
    T oldValue = cell.isEmpty() ? null : cell.getItem();

    if (!Objects.equals(oldValue, newValue))
      cell.commitEdit(newValue);

    ev.consume();
  }
}
