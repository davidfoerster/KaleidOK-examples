package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.StringConverter;


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

    public final Property<T> editorValue;

    public final StringConverter<T> converter;


    protected EditorNodeInfo( N node, Property<T> editorValue,
      StringConverter<T> converter )
    {
      this.node = node;
      this.editorValue = editorValue;
      this.converter = converter;
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      Property<T> editorValue, StringConverter<T> converter )
    {
      return (node != null) ?
        new EditorNodeInfo<>(node, editorValue, converter) :
        empty();
    }


    @SuppressWarnings("unchecked")
    public static <N extends Node, T> EditorNodeInfo<N, T> empty()
    {
      return (EditorNodeInfo<N, T>) EMPTY;
    }


    public boolean isEmpty()
    {
      return node == null;
    }
  }
}
