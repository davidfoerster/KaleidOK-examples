package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;


public class EditableTreeTableCell<T, N extends Node>
  extends TreeTableCell<ReadOnlyProperty<T>, T>
{
  private EditorNodeInfo<N, T> getEditorNodeInfo()
  {
    if (isEditableInherited())
    {
      TreeItem<ReadOnlyProperty<T>> item = getTreeTableRow().getTreeItem();
      if (item instanceof EditableTreeItem)
      {
        EditorNodeInfo<N, T> editorNodeInfo =
          ((EditableTreeItem<T, N>) item).getEditorNodeInfo();
        if (editorNodeInfo != null)
        {
          if (!editorNodeInfo.isEmpty())
            return editorNodeInfo;

          setEditable(false);
        }
      }
    }
    return null;
  }


  public N getEditorNode()
  {
    EditorNodeInfo<N, ?> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null) ? nodeInfo.node : null;
  }


  private Property<T> getEditorValue()
  {
    EditorNodeInfo<?, T> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null) ? nodeInfo.editorValue : null;
  }


  private String itemToString()
  {
    return itemToString(getItem(), isEmpty());
  }

  private String itemToString( T item, boolean empty )
  {
    if (item == null || empty)
      return null;

    EditorNodeInfo<?, T> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null && nodeInfo.converter != null) ?
      nodeInfo.converter.toString(item) :
      item.toString();
  }


  public boolean isEditableInherited()
  {
    return isEditable() &&
      getTableColumn().isEditable() &&
      getTreeTableView().isEditable();
  }


  @Override
  protected void updateItem( T item, boolean empty )
  {
    super.updateItem(item, empty);

    boolean editing = isEditing();
    setText(!editing ? itemToString(item, empty) : null);
    setGraphic((editing && !empty) ? getEditorNode() : null);
  }


  @Override
  public void startEdit()
  {
    startEdit2();
  }


  private boolean startEdit2()
  {
    if (!isEditing() && isEditableInherited())
    {
      Node node = getEditorNode();
      if (node != null)
      {
        super.startEdit();
        if (isEditing())
        {
          setText(null);

          if (!isEmpty())
          {
            T item = getItem();
            Property<T> editorValue = getEditorValue();
            if (item != null && editorValue != null)
              editorValue.setValue(item);
          }

          setGraphic(node);
          node.requestFocus();
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public void cancelEdit()
  {
    if (!isEditing())
      return;

    super.cancelEdit();
    setText(itemToString());
    setGraphic(null);
  }
}
