package kaleidok.javafx.scene.control.cell;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.value.ConstantObjectValue;

import java.util.function.BiConsumer;


public abstract class EditorNodeInfo<N extends Node, T>
{
  public interface ObservableGraphicsNode extends Observable
  {
    Node get( boolean editing );
  }


  public final N editorNode;

  public final boolean alwaysEditing;

  public final BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> valueChange;

  public final StringConverter<T> converter;


  public boolean isEmpty()
  {
    return editorNode == null;
  }


  public abstract ObservableGraphicsNode graphicsNodeProperty();


  public ObservableValue<? extends Node> notificationNodeProperty()
  {
    return ConstantObjectValue.empty();
  }


  protected EditorNodeInfo( N editorNode, boolean alwaysEditing,
    BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> valueChange,
    StringConverter<T> converter )
  {
    this.editorNode = editorNode;
    this.alwaysEditing = alwaysEditing;
    this.valueChange = valueChange;
    this.converter = converter;
  }


  public static <N extends Node, T> EditorNodeInfo<N, T> of( N editorNode,
    boolean alwaysEditing,
    BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> editorValue,
    StringConverter<T> converter )
  {
    return (editorNode != null) ?
      new SimpleEditorNodeInfo<>(editorNode, alwaysEditing,
        editorValue, converter) :
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
    return (EditorNodeInfo<N, T>) SimpleEditorNodeInfo.EMPTY;
  }
}
