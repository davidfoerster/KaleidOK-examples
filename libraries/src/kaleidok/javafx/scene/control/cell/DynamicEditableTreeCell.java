package kaleidok.javafx.scene.control.cell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.util.Callback;


public class DynamicEditableTreeCell<S, T, N extends Node>
  extends EditableTreeTableCell<S, T, N>
{
  public interface CellNodeFactory<S, T, N extends Node>
    extends Callback<DynamicEditableTreeCell<S,T,N>, EditorNodeInfo<N, T>>
  { }

  public final ObjectProperty<CellNodeFactory<S,T,N>> cellNodeFactory;


  public DynamicEditableTreeCell( CellNodeFactory<S,T,N> cellNodeFactory )
  {
    this.cellNodeFactory = new SimpleObjectProperty<>(
      this, "cell node factory", cellNodeFactory);
  }


  public DynamicEditableTreeCell()
  {
    this(null);
  }


  @Override
  protected EditorNodeInfo<N, T> makeEditorNode()
  {
    CellNodeFactory<S,T,N> cellNodeFactory = this.cellNodeFactory.get();
    return (cellNodeFactory != null) ? cellNodeFactory.call(this) : null;
  }
}
