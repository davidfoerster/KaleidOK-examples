package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableStringValue;
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

    public final ReadOnlyProperty<T> editorValue;

    public final ObservableStringValue editorStringValue;


    protected EditorNodeInfo( N node, ReadOnlyProperty<T> editorValue,
      ObservableStringValue editorStringValue )
    {
      this.node = node;
      this.editorValue = editorValue;
      this.editorStringValue = editorStringValue;
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      ReadOnlyProperty<T> editorValue, ObservableStringValue editorStringValue )
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
        editorValue),
        editorStringValue);
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
        (node == otherENI.node && editorValue == otherENI.editorValue &&
          editorStringValue == otherENI.editorStringValue);
    }


    public boolean isEmpty()
    {
      return node == null;
    }
  }
}
