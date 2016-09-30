package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.Objects;


public abstract class EditableTreeTableCell<S, T, N extends Node>
  extends TreeTableCell<S, T>
{
  protected N editorNode = null;

  private Property<T> editorValue = null;


  protected EditableTreeTableCell()
  {
    setOnMouseClicked(DefaultMouseHandler.INSTANCE);
    setOnKeyTyped(DefaultKeyHandler.INSTANCE);
  }


  protected abstract EditorNodeInfo<N, T> makeEditorNode();


  @Override
  protected void updateItem( T item, boolean empty )
  {
    super.updateItem(item, empty);

    boolean editing = isEditing();
    setText((!empty && !editing && item != null) ? item.toString() : null);
    setGraphic((!empty && editing) ? editorNode : null);
  }


  public N getEditorNode()
  {
    if (editorNode == null && isEditable())
    {
      EditorNodeInfo<N, T> nodeInfo = makeEditorNode();
      if (nodeInfo != null)
      {
        if (nodeInfo.node != null)
        {
          editorNode = nodeInfo.node;
          editorValue = nodeInfo.editorValue;
        }
        else
        {
          setEditable(false);
        }
      }
    }
    return editorNode;
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
          if (editorValue != null)
            editorValue.setValue(!isEmpty() ? getItem() : null);
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
    //if (editorValue != null)
      //setItem(editorValue.getValue());

    T item = getItem();
    setText((item != null && !isEmpty()) ? item.toString() : null);
    setGraphic(null);
  }


  protected static class DefaultMouseHandler
    implements EventHandler<MouseEvent>
  {
    public static DefaultMouseHandler INSTANCE = new DefaultMouseHandler();

    @Override
    public void handle( MouseEvent ev )
    {
      if (ev.getEventType() == MouseEvent.MOUSE_PRESSED &&
        ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2)
      {
        if (((EditableTreeTableCell<?,?,?>) ev.getTarget()).startEdit2())
          ev.consume();
      }
    }
  }


  protected static class DefaultKeyHandler implements EventHandler<KeyEvent>
  {
    public static final DefaultKeyHandler INSTANCE = new DefaultKeyHandler();

    @Override
    public void handle( KeyEvent ev )
    {
      if (ev.getEventType() == KeyEvent.KEY_TYPED)
      {
        switch (ev.getCode())
        {
        case ENTER:
        case F2:
          if (((EditableTreeTableCell<?, ?, ?>) ev.getTarget()).startEdit2())
            ev.consume();
          break;
        }
      }
    }
  }


  public static class EditorNodeInfo<N extends Node, T>
  {
    private static EditorNodeInfo<?, ?> EMPTY =
      new EditorNodeInfo<>(null, null);

    public final N node;

    public final Property<T> editorValue;


    protected EditorNodeInfo( N node, Property<T> editorValue )
    {
      this.node = node;
      this.editorValue = editorValue;
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      Property<T> editorValue )
    {
      return (node != null) ?
        new EditorNodeInfo<>(node, editorValue) :
        empty();
    }


    @SuppressWarnings("unchecked")
    public static <N extends Node, T> EditorNodeInfo<N, T> empty()
    {
      return (EditorNodeInfo<N, T>) EMPTY;
    }


    @Override
    public int hashCode()
    {
      return kaleidok.util.Objects.hashCode(Objects.hashCode(node), editorValue);
    }


    @Override
    public boolean equals( Object other )
    {
      if (other == this)
        return true;
      if (!(other instanceof EditorNodeInfo))
        return false;

      EditorNodeInfo<?,?> otherENI = (EditorNodeInfo<?, ?>) other;
      return node == otherENI.node && editorValue == otherENI.editorValue;
    }


    public boolean isEmpty()
    {
      return node == null;
    }
  }
}
