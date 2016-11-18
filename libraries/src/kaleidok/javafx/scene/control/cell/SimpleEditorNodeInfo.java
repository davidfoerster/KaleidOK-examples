package kaleidok.javafx.scene.control.cell;

import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.util.StringConverter;

import java.util.function.BiConsumer;


public class SimpleEditorNodeInfo<N extends Node, T>
  extends EditorNodeInfo<N, T> implements EditorNodeInfo.ObservableGraphicsNode
{
  static final SimpleEditorNodeInfo<?, ?> EMPTY =
    new SimpleEditorNodeInfo<>(null, false, null, null);


  protected SimpleEditorNodeInfo( N editorNode, boolean alwaysEditing,
    BiConsumer<? super EditableTreeTableCell<? super T, N>, ? super T> valueChange,
    StringConverter<T> converter )
  {
    super(editorNode, alwaysEditing, valueChange, converter);
  }


  @Override
  public ObservableGraphicsNode graphicsNodeProperty()
  {
    return this;
  }


  @Override
  public Node get( boolean editing )
  {
    return editing ? editorNode : null;
  }


  @Override
  public void addListener( InvalidationListener listener ) { }


  @Override
  public void removeListener( InvalidationListener listener ) { }
}
