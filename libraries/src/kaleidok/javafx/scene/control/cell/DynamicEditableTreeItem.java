package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.util.Callback;


public class DynamicEditableTreeItem<T, N extends Node>
  extends EditableTreeItem<T, N>
{
  public interface TreeItemProvider<T, N extends Node>
    extends Callback<DynamicEditableTreeItem<?, ?>, EditorNodeInfo<N, T>>
  { }

  private TreeItemProvider<T,N> cellNodeFactory;

  private EditorNodeInfo<N, T> editorNodeInfo = null;


  public DynamicEditableTreeItem( ReadOnlyProperty<T> value,
    TreeItemProvider<T, N> treeItemProvider )
  {
    super(value);
    this.cellNodeFactory = treeItemProvider;
  }


  @Override
  public EditorNodeInfo<N, T> getEditorNodeInfo()
  {
    if (editorNodeInfo == null)
    {
      TreeItemProvider<T, N> treeItemProvider = cellNodeFactory;
      editorNodeInfo =
        (treeItemProvider != null) ? treeItemProvider.call(this) : null;
    }
    return editorNodeInfo;
  }


  public TreeItemProvider<T, N> getCellNodeFactory()
  {
    return cellNodeFactory;
  }

  public void setCellNodeFactory(
    TreeItemProvider<T, N> cellNodeFactory )
  {
    if (editorNodeInfo == null)
      this.cellNodeFactory = cellNodeFactory;

    throw new IllegalStateException(
      "The current cell node factory was already used successfully");
  }
}
