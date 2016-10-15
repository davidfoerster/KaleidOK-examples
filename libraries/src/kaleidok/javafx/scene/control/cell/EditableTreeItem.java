package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import kaleidok.util.Objects;


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


  public static class EditorNodeInfo<N extends Node, T>
  {
    private static EditorNodeInfo<?, ?> EMPTY =
      new EditorNodeInfo<>(null, null, null);

    public final N node;

    public final ReadOnlyProperty<T> value;

    public final ObservableValue<String> stringValue;


    protected EditorNodeInfo( N node, ReadOnlyProperty<T> value,
      ObservableValue<String> stringValue )
    {
      this.node = node;
      this.value = value;
      this.stringValue = stringValue;
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      ReadOnlyProperty<T> editorValue, ObservableValue<String> editorStringValue )
    {
      return (node != null) ?
        new EditorNodeInfo<>(node, editorValue, editorStringValue) :
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
      return Objects.hashCode(Objects.hashCode(
        java.util.Objects.hashCode(node),
        value),
        stringValue);
    }


    @Override
    public boolean equals( Object other )
    {
      if (other == this)
        return true;
      if (!(other instanceof EditorNodeInfo))
        return false;

      EditorNodeInfo<?,?> otherENI = (EditorNodeInfo<?, ?>) other;
      return (isEmpty() && otherENI.isEmpty()) ||
        (node == otherENI.node && value == otherENI.value &&
          stringValue == otherENI.stringValue);
    }


    public boolean isEmpty()
    {
      return node == null;
    }
  }
}
