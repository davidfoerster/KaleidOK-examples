package kaleidok.javafx.scene.control.cell.factory;

import javafx.beans.property.Property;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import kaleidok.javafx.beans.property.BoundedValue;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeCell;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell.EditorNodeInfo;


public abstract class CellSpinnerFactory<T extends Number, P extends Property<Number>>
  extends TypedTreeTableCellFactory<P, T, Spinner<T>>
{
  protected CellSpinnerFactory( Class<? extends P> treeItemValueClass )
  {
    super(null, treeItemValueClass);
  }


  @Override
  protected EditorNodeInfo<Spinner<T>, T> callTypeChecked(
    DynamicEditableTreeCell<P, T, Spinner<T>> cell )
  {
    Spinner<T> editor = new Spinner<>(makeValueFactory(
      cell.getTreeTableRow().getTreeItem().getValue()));
    Property<T> valueProperty = editor.getValueFactory().valueProperty();
    return EditorNodeInfo.of(editor, valueProperty);
  }


  protected abstract SpinnerValueFactory<T> makeValueFactory( P property );


  public static class BoundedValueCellSpinnerFactory<T extends Number>
    extends CellSpinnerFactory<T, BoundedValue<T, ? extends SpinnerValueFactory<T>>>
  {
    public static final BoundedValueCellSpinnerFactory<? extends Number> INSTANCE =
      new BoundedValueCellSpinnerFactory<>();


    @SuppressWarnings("unchecked")
    public static <T extends Number> BoundedValueCellSpinnerFactory<T> getInstance()
    {
      return (BoundedValueCellSpinnerFactory<T>) INSTANCE;
    }


    @SuppressWarnings({ "unchecked", "RedundantCast" })
    protected BoundedValueCellSpinnerFactory()
    {
      super((Class<? extends BoundedValue<T, ? extends SpinnerValueFactory<T>>>) (Class<?>) BoundedValue.class);
    }


    @Override
    protected SpinnerValueFactory<T> makeValueFactory(
      BoundedValue<T, ? extends SpinnerValueFactory<T>> property )
    {
      return property.getBounds();
    }
  }
}
