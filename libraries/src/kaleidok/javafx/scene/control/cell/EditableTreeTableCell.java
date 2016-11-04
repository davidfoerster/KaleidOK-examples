package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;

import java.util.function.BiConsumer;


public class EditableTreeTableCell<T, N extends Node>
  extends TreeTableCell<ReadOnlyProperty<T>, T>
{
  private EditorNodeInfo<N, T> nodeInfo = null;


  private void updateNodeInfo( TreeItem<ReadOnlyProperty<T>> item )
  {
    if (item instanceof EditableTreeItem && isEditableInherited())
    {
      EditorNodeInfo<N, T> nodeInfo =
        ((EditableTreeItem<T, N>) item).getEditorNodeInfo();
      if (nodeInfo != null)
      {
        if (!nodeInfo.isEmpty())
        {
          this.nodeInfo = nodeInfo;
          return;
        }

        setEditable(false);
      }
    }
    this.nodeInfo = null;
  }


  @Override
  public void updateIndex( int i )
  {
    updateNodeInfo(getTreeTableView().getTreeItem(i));
    super.updateIndex(i);
    if (isAlwaysEditing())
      startEdit2(false);
  }


  public N getEditorNode()
  {
    EditorNodeInfo<N, ?> nodeInfo = this.nodeInfo;
    return (nodeInfo != null) ? nodeInfo.node : null;
  }


  private boolean isAlwaysEditing()
  {
    EditorNodeInfo<N, ?> nodeInfo = this.nodeInfo;
    return nodeInfo != null && nodeInfo.alwaysEditing;
  }


  private BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T>
  getValueChangeListener()
  {
    EditorNodeInfo<N, T> nodeInfo = this.nodeInfo;
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

    EditorNodeInfo<?, T> nodeInfo = this.nodeInfo;
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
    startEdit2(true);
  }


  private boolean startEdit2( boolean requestFocus )
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
          if (requestFocus)
            node.requestFocus();
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public void commitEdit( T newValue )
  {
    if (!isEditing())
      return;

    if (isAlwaysEditing())
    {
      //noinspection OverlyStrongTypeCast
      ((Property<T>) getTreeTableRow().getTreeItem().getValue())
        .setValue(newValue);
    }
    else
    {
      super.commitEdit(newValue);
    }
  }


  @Override
  public void cancelEdit()
  {
    if (!isEditing())
      return;

    if (!isAlwaysEditing())
    {
      super.cancelEdit();
      setText(itemToString(getItem(), isEmpty()));
      setGraphic(null);
    }
  }
}
