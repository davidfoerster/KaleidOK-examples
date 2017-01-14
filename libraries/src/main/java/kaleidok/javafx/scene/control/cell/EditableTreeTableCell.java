package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import java.util.function.BiConsumer;


public class EditableTreeTableCell<T, N extends Node>
  extends NodeInfoTreeTableCell<T, T, N>
{
  @Override
  protected EditorNodeInfo<N, T> updateNodeInfo(
    TreeItem<ReadOnlyProperty<T>> newItem, EditorNodeInfo<N, T> oldNodeInfo,
    EditorNodeInfo<N, T> newNodeInfo )
  {
    if (!isEditableInherited())
      return null;

    if (newNodeInfo != null && newNodeInfo.isEmpty())
      setEditable(false);

    return newNodeInfo;
  }


  @Override
  public void updateIndex( int i )
  {
    super.updateIndex(i);
    if (isAlwaysEditing())
      startEdit(false);
  }


  private void updateEditorValue( T item )
  {
    BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> changeListener =
      getValueChangeListener();
    if (changeListener != null)
      changeListener.accept(this, item);
  }


  @Override
  protected void updateItem( T item, boolean empty )
  {
    super.updateItem(item, empty);

    if (!empty)
      updateEditorValue(item);

    updateGraphics(item, empty, isEditing());
  }


  private void updateGraphics( T item, boolean empty, boolean editing )
  {
    setText(!editing ? itemToString(item, empty) : null);
    setGraphic(!empty ? getGraphicsNode(editing) : null);
  }


  @Override
  public void startEdit()
  {
    startEdit(true);
  }


  private boolean startEdit( boolean requestFocus )
  {
    if (!isEditing() && isEditableInherited())
    {
      Node graphicsNode = getGraphicsNode(true);
      if (graphicsNode != null)
      {
        super.startEdit();
        if (isEditing())
        {
          setText(null);
          updateEditorValue(getItem());
          setGraphic(graphicsNode);

          if (requestFocus)
          {
            Node editorNode = getEditorNode();
            if (editorNode != null)
              editorNode.requestFocus();
          }

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
      updateGraphics(getItem(), isEmpty(), false);
    }
  }
}
