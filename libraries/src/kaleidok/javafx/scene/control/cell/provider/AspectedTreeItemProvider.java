package kaleidok.javafx.scene.control.cell.provider;

import javafx.scene.Node;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;

import java.util.Objects;


public abstract class AspectedTreeItemProvider<T, N extends Node, A, Tag extends PropertyAspectTag<A, ? super T>>
  extends FilteredTreeItemProvider<T, N>
{
  public final Tag aspectTag;


  protected AspectedTreeItemProvider( Tag aspectTag )
  {
    this.aspectTag = aspectTag;
  }


  @Override
  public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
  {
    //noinspection unchecked
    return item.getValue() instanceof AspectedReadOnlyProperty &&
      (aspectTag == null ||
         getAspect((DynamicEditableTreeItem<T, N>) item) != null);
  }


  protected A getAspect( DynamicEditableTreeItem<T, N> item )
  {
    return ((AspectedReadOnlyProperty<T>) item.getValue())
      .getAspect(Objects.requireNonNull(aspectTag));
  }
}
