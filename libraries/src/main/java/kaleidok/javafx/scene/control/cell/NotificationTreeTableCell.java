package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;


public class NotificationTreeTableCell<T, U, N extends Node>
  extends NodeInfoTreeTableCell<T, U, N>
{
  @Override
  protected EditorNodeInfo<N, T> updateNodeInfo(
    TreeItem<ReadOnlyProperty<T>> newItem, EditorNodeInfo<N, T> oldNodeInfo,
    EditorNodeInfo<N, T> newNodeInfo )
  {
    graphicProperty().unbind();

    if (newNodeInfo != null && !newNodeInfo.isEmpty())
      graphicProperty().bind(newNodeInfo.notificationNodeProperty());

    return newNodeInfo;
  }


  @Override
  protected void updateItem( U item, boolean empty )
  {
    if (item == getItem())
      return;

    super.updateItem(item, empty);

    setText((item != null) ? item.toString() : null);
    Property<Node> graphic = graphicProperty();
    if (!graphic.isBound())
      graphic.setValue(null);
  }
}
