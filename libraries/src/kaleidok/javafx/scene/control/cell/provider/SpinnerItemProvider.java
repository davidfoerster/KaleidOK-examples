package kaleidok.javafx.scene.control.cell.provider;

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
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;
import kaleidok.util.Math;

import java.util.Objects;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public abstract class SpinnerItemProvider<T extends Number>
  extends FilteredTreeItemProvider<Number, Spinner<T>>
{
  @Override
  protected EditorNodeInfo<Spinner<T>, Number> callTypeChecked(
    DynamicEditableTreeItem<Number, Spinner<T>> item )
  {
    SpinnerValueFactory<T> svf =
      getValueFactory((AspectedProperty<Number>) item.getValue());
    Spinner<T> spinner = makeSpinner(svf);
    spinner.setEditable(true);
    spinner.setOnScroll(SpinnerItemProvider::handleScrollEvent);
    EventUtils.chain(spinner.getEditor().onActionProperty(),
      SpinnerItemProvider::handleActionEvent);

    //noinspection unchecked
    return EditorNodeInfo.of(spinner, null,
      (StringConverter<Number>) svf.getConverter());
  }


  protected Spinner<T> makeSpinner( SpinnerValueFactory<T> valueFactory )
  {
    return new Spinner<>(valueFactory);
  }


  protected abstract SpinnerValueFactory<T> getValueFactory(
    AspectedProperty<Number> property );


  public static class BoundedValueSpinnerItemProvider<T extends Number>
    extends SpinnerItemProvider<T>
  {
    @Override
    public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
    {
      return BoundedValueTag.getInstance().ofAny(item.getValue()) != null;
    }


    @Override
    protected SpinnerValueFactory<T> getValueFactory(
      AspectedProperty<Number> property )
    {
      return property.getAspect(BoundedValueTag.getInstance());
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
    Cell<T> cell = (Cell<T>)
      Objects.requireNonNull(findParentCell(spinner.getParent()));
    T oldValue = cell.isEmpty() ? null : cell.getItem();
    if (Objects.equals(oldValue, newValue))
      newValue = oldValue;
    cell.commitEdit(newValue);

    ev.consume();
  }
}
