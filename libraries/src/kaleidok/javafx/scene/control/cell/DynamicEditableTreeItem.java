package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.util.Callback;


public class DynamicEditableTreeItem<T, N extends Node>
  extends EditableTreeItem<T, N>
{
  public interface TreeItemProvider<T, N extends Node>
    extends Callback<DynamicEditableTreeItem<?, ?>, EditorNodeInfo<N, T>>
  { }

  public final ObjectProperty<TreeItemProvider<T,N>> cellNodeFactory;

  private EditorNodeInfo<N, T> editorNodeInfo = null;


  public DynamicEditableTreeItem( ReadOnlyProperty<T> value, TreeItemProvider<T, N> treeItemProvider )
  {
    super(value);
    this.cellNodeFactory = new SimpleObjectProperty<>(
      this, "cell node provider", treeItemProvider);
  }


  @Override
  public EditorNodeInfo<N, T> getEditorNodeInfo()
  {
    if (editorNodeInfo == null)
    {
      TreeItemProvider<T, N> treeItemProvider = this.cellNodeFactory.get();
      editorNodeInfo =
        (treeItemProvider != null) ? treeItemProvider.call(this) : null;
    }
    return editorNodeInfo;
  }
}
