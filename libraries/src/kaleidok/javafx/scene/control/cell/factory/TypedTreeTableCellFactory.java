package kaleidok.javafx.scene.control.cell.factory;

import javafx.scene.Node;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeCell;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell.EditorNodeInfo;


public abstract class TypedTreeTableCellFactory<S, T, N extends Node>
  implements DynamicEditableTreeCell.CellNodeFactory<S, T, N>
{
  public final Class<? extends T> itemValueClass;

  public final Class<? extends S> treeItemValueClass;


  protected TypedTreeTableCellFactory( Class<? extends T> itemValueClass,
    Class<? extends S> treeItemValueClass )
  {
    if (itemValueClass == null && treeItemValueClass == null)
    {
      throw new NullPointerException(
        "At least one class must be non-null");
    }

    this.itemValueClass = itemValueClass;
    this.treeItemValueClass = treeItemValueClass;
  }


  public boolean isApplicable( DynamicEditableTreeCell<S, T, N> cell )
  {
    return !cell.isEmpty() &&
      (itemValueClass == null ||
         itemValueClass.isInstance(cell.getItem())) &&
      (treeItemValueClass == null ||
         treeItemValueClass.isInstance(
           cell.getTreeTableRow().getTreeItem().getValue()));
  }


  @Override
  public EditorNodeInfo<N, T> call( DynamicEditableTreeCell<S, T, N> cell )
  {
    return isApplicable(cell) ? callTypeChecked(cell) : null;
  }


  protected abstract EditorNodeInfo<N, T> callTypeChecked(
    DynamicEditableTreeCell<S, T, N> cell );
}
