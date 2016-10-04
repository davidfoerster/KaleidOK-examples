package kaleidok.javafx.scene.control.cell.factory;

import javafx.scene.Node;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;


public abstract class FilteredTreeItemProvider<T, N extends Node>
  implements DynamicEditableTreeItem.TreeItemProvider<T, N>
{
  public abstract boolean isApplicable( DynamicEditableTreeItem<T, N> item );


  @Override
  public EditorNodeInfo<N, T> call( DynamicEditableTreeItem<T, N> cell )
  {
    return isApplicable(cell) ? callTypeChecked(cell) : null;
  }


  protected abstract EditorNodeInfo<N, T> callTypeChecked(
    DynamicEditableTreeItem<T, N> cell );
}
