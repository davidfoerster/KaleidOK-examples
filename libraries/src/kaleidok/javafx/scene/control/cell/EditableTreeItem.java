package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.StringConverter;

import java.util.function.BiConsumer;


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
      new EditorNodeInfo<>(null, false, null, null);

    public final N node;

    public final boolean alwaysEditing;

    public final BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> valueChange;

    public final StringConverter<T> converter;


    protected EditorNodeInfo( N node, boolean alwaysEditing,
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> valueChange,
      StringConverter<T> converter )
    {
      this.node = node;
      this.alwaysEditing = alwaysEditing;
      this.valueChange = valueChange;
      this.converter = converter;
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      boolean alwaysEditing,
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> editorValue,
      StringConverter<T> converter )
    {
      return (node != null) ?
        new EditorNodeInfo<>(node, alwaysEditing, editorValue, converter) :
        empty();
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> editorValue,
      StringConverter<T> converter )
    {
      return of(node, false, editorValue, converter);
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      boolean alwaysEditing,
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> editorValue )
    {
      return of(node, alwaysEditing, editorValue, null);
    }


    public static <N extends Node, T> EditorNodeInfo<N, T> of( N node,
      BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> editorValue )
    {
      return of(node, editorValue, null);
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
