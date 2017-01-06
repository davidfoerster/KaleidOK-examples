package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;


public abstract class EditableTreeItem<T, N extends Node>
  extends TreeItem<ReadOnlyProperty<T>>
{
  protected EditableTreeItem( ReadOnlyProperty<T> value )
  {
    super(value);
  }


  protected EditableTreeItem( ReadOnlyProperty<T> value, Node graphic )
  {
    super(value, graphic);
  }


  public abstract EditorNodeInfo<N, T> getEditorNodeInfo();
}
