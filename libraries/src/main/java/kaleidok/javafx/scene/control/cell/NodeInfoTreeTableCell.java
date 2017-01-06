package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.function.BiConsumer;


public abstract class NodeInfoTreeTableCell<T, U, N extends Node>
  extends TreeTableCell<ReadOnlyProperty<T>, U>
{
  private EditorNodeInfo<N, T> nodeInfo = null;


  protected final EditorNodeInfo<N, T> getNodeInfo()
  {
    return nodeInfo;
  }


  protected abstract EditorNodeInfo<N, T> updateNodeInfo(
    TreeItem<ReadOnlyProperty<T>> newItem, EditorNodeInfo<N, T> oldNodeInfo,
    EditorNodeInfo<N, T> newNodeInfo );


  @Override
  @OverridingMethodsMustInvokeSuper
  public void updateIndex( int i )
  {
    TreeItem<ReadOnlyProperty<T>> newItem = getTreeTableView().getTreeItem(i);
    EditorNodeInfo<N, T> newNodeInfo =
      (newItem instanceof EditableTreeItem) ?
        ((EditableTreeItem<T, N>) newItem).getEditorNodeInfo() :
        null;
    newNodeInfo = updateNodeInfo(newItem, this.nodeInfo, newNodeInfo);
    this.nodeInfo =
      (newNodeInfo != null && !newNodeInfo.isEmpty()) ? newNodeInfo : null;

    super.updateIndex(i);
  }


  public boolean isEditableInherited()
  {
    return isEditable() &&
      getTableColumn().isEditable() &&
      getTreeTableView().isEditable();
  }


  public N getEditorNode()
  {
    EditorNodeInfo<N, ?> nodeInfo = getNodeInfo();
    return (nodeInfo != null) ? nodeInfo.editorNode : null;
  }


  protected Node getGraphicsNode( boolean editing )
  {
    EditorNodeInfo<?, ?> nodeInfo = getNodeInfo();
    return (nodeInfo != null) ?
      nodeInfo.graphicsNodeProperty().get(editing) :
      null;
  }


  protected boolean isAlwaysEditing()
  {
    EditorNodeInfo<N, ?> nodeInfo = getNodeInfo();
    return nodeInfo != null && nodeInfo.alwaysEditing;
  }


  protected BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T>
  getValueChangeListener()
  {
    EditorNodeInfo<N, T> nodeInfo = getNodeInfo();
    return (nodeInfo != null) ? nodeInfo.valueChange : null;
  }


  protected String itemToString( T item, boolean empty )
  {
    if (item == null || empty)
      return null;

    EditorNodeInfo<?, T> nodeInfo = getNodeInfo();
    return (nodeInfo != null && nodeInfo.converter != null) ?
      nodeInfo.converter.toString(item) :
      item.toString();
  }
}
