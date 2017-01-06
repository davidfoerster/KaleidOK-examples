package kaleidok.javafx.scene.control.cell.provider;

import javafx.scene.Node;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;


public abstract class FilteredTreeItemProvider<T, N extends Node>
  implements DynamicEditableTreeItem.TreeItemProvider<T, N>
{
  public abstract boolean isApplicable( DynamicEditableTreeItem<?, ?> item );


  @Override
  public EditorNodeInfo<N, T> call( DynamicEditableTreeItem<?, ?> item )
  {
    //noinspection unchecked
    return isApplicable(item) ?
      callTypeChecked((DynamicEditableTreeItem<T, N>) item) :
      null;
  }


  protected abstract EditorNodeInfo<N, T> callTypeChecked(
    DynamicEditableTreeItem<T, N> item );
}
