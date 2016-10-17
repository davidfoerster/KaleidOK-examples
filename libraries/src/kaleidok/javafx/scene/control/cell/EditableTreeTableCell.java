package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;

import java.util.function.BiConsumer;


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


  private BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T>
  getValueChangeListener()
  {
    EditorNodeInfo<N, T> nodeInfo = getEditorNodeInfo();
    return (nodeInfo != null) ? nodeInfo.valueChange : null;
  }


  private void updateEditorValue( T item, boolean empty )
  {
    if (!empty)
    {
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> changeListener =
        getValueChangeListener();
      if (changeListener != null)
        changeListener.accept(this, item);
    }
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
    updateEditorValue(item, empty);

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
          updateEditorValue(getItem(), isEmpty());
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
    setText(itemToString(getItem(), isEmpty()));
    setGraphic(null);
  }
}
