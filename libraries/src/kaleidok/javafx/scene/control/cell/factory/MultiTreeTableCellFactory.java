package kaleidok.javafx.scene.control.cell.factory;

import javafx.scene.Node;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeCell;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeCell.CellNodeFactory;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;


public class MultiTreeTableCellFactory<S, T, N extends Node>
  implements CellNodeFactory<S, T, N>, List<CellNodeFactory<S, T, N>>
{
  private final List<CellNodeFactory<S, T, N>> underlying;


  @SuppressWarnings("unchecked")
  public MultiTreeTableCellFactory(
    List<? extends CellNodeFactory<S, T, N>> underlying )
  {
    this.underlying =
      (List<CellNodeFactory<S, T, N>>) underlying;
  }


  public MultiTreeTableCellFactory()
  {
    underlying = new ArrayList<>();
  }


  @Override
  public EditableTreeTableCell.EditorNodeInfo<N, T> call(
    final DynamicEditableTreeCell<S, T, N> cell )
  {
    return this.stream()
      .map((cnf) -> cnf.call(cell))
      .filter(Objects::nonNull)
      .findFirst().orElse(null);
  }


  @Override
  public int size()
  {
    return underlying.size();
  }

  @Override
  public boolean isEmpty()
  {
    return underlying.isEmpty();
  }

  @Override
  public boolean contains( Object o )
  {
    return underlying.contains(o);
  }

  @Override
  public Iterator<CellNodeFactory<S, T, N>> iterator()
  {
    return underlying.iterator();
  }

  @Override
  public Object[] toArray()
  {
    return underlying.toArray();
  }

  @SuppressWarnings("SuspiciousToArrayCall")
  @Override
  public <E> E[] toArray( E[] a )
  {
    return underlying.toArray(a);
  }

  @Override
  public boolean add(
    CellNodeFactory<S, T, N> callback )
  {
    return underlying.add(callback);
  }

  @Override
  public boolean remove( Object o )
  {
    return underlying.remove(o);
  }

  @Override
  public boolean containsAll( Collection<?> c )
  {
    return underlying.containsAll(c);
  }

  @Override
  public boolean addAll(
    Collection<? extends CellNodeFactory<S, T, N>> c )
  {
    return underlying.addAll(c);
  }

  @Override
  public boolean addAll( int index,
    Collection<? extends CellNodeFactory<S, T, N>> c )
  {
    return underlying.addAll(index, c);
  }

  @Override
  public boolean removeAll( Collection<?> c )
  {
    return underlying.removeAll(c);
  }

  @Override
  public boolean retainAll( Collection<?> c )
  {
    return underlying.retainAll(c);
  }

  @Override
  public void replaceAll( UnaryOperator<CellNodeFactory<S, T, N>> operator )
  {
    underlying.replaceAll(operator);
  }

  @Override
  public void sort( Comparator<? super CellNodeFactory<S, T, N>> c )
  {
    underlying.sort(c);
  }

  @Override
  public void clear()
  {
    underlying.clear();
  }

  @Override
  public boolean equals( Object o )
  {
    return (o instanceof MultiTreeTableCellFactory) &&
      underlying.equals(((MultiTreeTableCellFactory<?,?,?>) o).underlying);
  }

  @Override
  public int hashCode()
  {
    return underlying.hashCode();
  }

  @Override
  public CellNodeFactory<S, T, N> get( int index )
  {
    return underlying.get(index);
  }

  @Override
  public CellNodeFactory<S, T, N> set( int index,
    CellNodeFactory<S, T, N> element )
  {
    return underlying.set(index, element);
  }

  @Override
  public void add( int index,
    CellNodeFactory<S, T, N> element )
  {
    underlying.add(index, element);
  }

  @Override
  public CellNodeFactory<S, T, N> remove( int index )
  {
    return underlying.remove(index);
  }

  @Override
  public int indexOf( Object o )
  {
    return underlying.indexOf(o);
  }

  @Override
  public int lastIndexOf( Object o )
  {
    return underlying.lastIndexOf(o);
  }

  @Override
  public ListIterator<CellNodeFactory<S, T, N>> listIterator()
  {
    return underlying.listIterator();
  }

  @Override
  public ListIterator<CellNodeFactory<S, T, N>> listIterator( int index )
  {
    return underlying.listIterator(index);
  }

  @Override
  public List<CellNodeFactory<S, T, N>> subList( int fromIndex, int toIndex )
  {
    return underlying.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<CellNodeFactory<S, T, N>> spliterator()
  {
    return underlying.spliterator();
  }

  @Override
  public boolean removeIf(
    Predicate<? super CellNodeFactory<S, T, N>> filter )
  {
    return underlying.removeIf(filter);
  }

  @Override
  public Stream<CellNodeFactory<S, T, N>> stream()
  {
    return underlying.stream();
  }

  @Override
  public Stream<CellNodeFactory<S, T, N>> parallelStream()
  {
    return underlying.parallelStream();
  }

  @Override
  public void forEach(
    Consumer<? super CellNodeFactory<S, T, N>> action )
  {
    underlying.forEach(action);
  }
}
