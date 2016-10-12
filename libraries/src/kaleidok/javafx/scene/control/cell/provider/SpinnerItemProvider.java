package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import kaleidok.javafx.beans.property.AspectedProperty;
import kaleidok.javafx.beans.property.aspect.bounded.BoundedValueTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;
import kaleidok.util.Math;


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
    spinner.setOnScroll((ev) -> {
        int exponent = ev.isShiftDown() ? 1 : 0;
        if (ev.isControlDown())
          exponent += 2;
        ((Spinner<?>) ev.getSource()).increment(
          (int)(ev.getDeltaY() / ev.getMultiplierY() * Math.pow10(exponent)));
        ev.consume();
      });
    return spinner;
  }


  protected abstract SpinnerValueFactory<T> getValueFactory( ReadOnlyProperty<Number> property );


  public static class BoundedValueSpinnerItemProvider<T extends Number>
    extends SpinnerItemProvider<T>
  {
    @Override
    public boolean isApplicable( DynamicEditableTreeItem<Number, Spinner<T>> item )
    {
      ReadOnlyProperty<Number> value = item.getValue();
      //noinspection OverlyStrongTypeCast
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
}
