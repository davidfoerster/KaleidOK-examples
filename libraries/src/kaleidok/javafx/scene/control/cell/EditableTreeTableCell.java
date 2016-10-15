package kaleidok.javafx.scene.control.cell;

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


  private ReadOnlyProperty<T> getEditorValue()
  {
    EditorNodeInfo<N, T> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null) ? nodeInfo.value : null;
  }


  private String getEditorText()
  {
    return getEditorText(getItem(), isEmpty());
  }

  private String getEditorText( T item, boolean empty )
  {
    if (item == null || empty)
      return null;

    EditorNodeInfo<N, T> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null && nodeInfo.stringValue != null) ?
      nodeInfo.stringValue.getValue() :
      item.toString();
  }


  @Override
  protected void updateItem( T item, boolean empty )
  {
    super.updateItem(item, empty);

    boolean editing = isEditing();
    setText(!editing ? getEditorText(item, empty) : null);
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
      N node = getEditorNode();
      if (node != null)
      {
        super.startEdit();
        if (isEditing())
        {
          setText(null);

          // TODO: Is this part necessary?
          //Property<S> value = getEditorValue();
          //if (value != null)
            //value.setValue(!isEmpty() ? getItem() : null);

          setGraphic(node);
          node.requestFocus();
          return true;
        }
      }
    }
    return false;
  }


  public boolean isEditableInherited()
  {
    return isEditable() &&
      getTableColumn().isEditable() &&
      getTreeTableView().isEditable();
  }


  @Override
  public void cancelEdit()
  {
    if (!isEditing())
      return;

    super.cancelEdit();

    // TODO: Is this really necessary even though the cell item value is bound to the spinner value?
    //if (value != null)
      //setItem(value.getValue());

    setText(getEditorText());
    setGraphic(null);
  }
}
