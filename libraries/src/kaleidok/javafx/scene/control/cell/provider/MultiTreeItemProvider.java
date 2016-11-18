package kaleidok.javafx.scene.control.cell.provider;

import javafx.scene.Node;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;


public class MultiTreeItemProvider<T, N extends Node>
  implements TreeItemProvider<T, N>, List<TreeItemProvider<T, N>>
{
  private final List<TreeItemProvider<T, N>> underlying;


  @SuppressWarnings("unchecked")
  public MultiTreeItemProvider(
    List<? extends TreeItemProvider<T, N>> underlying )
  {
    this.underlying =
      (List<TreeItemProvider<T, N>>) underlying;
  }


  public MultiTreeItemProvider()
  {
    underlying = new ArrayList<>();
  }


  @Override
  public EditorNodeInfo<N, T> call( final DynamicEditableTreeItem<?, ?> item )
  {
    return this.stream()
      .map((cnf) -> cnf.call(item))
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
  public Iterator<TreeItemProvider<T, N>> iterator()
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
  public boolean add( TreeItemProvider<T, N> callback )
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
    Collection<? extends TreeItemProvider<T, N>> c )
  {
    return underlying.addAll(c);
  }

  @Override
  public boolean addAll( int index,
    Collection<? extends TreeItemProvider<T, N>> c )
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
  public void replaceAll( UnaryOperator<TreeItemProvider<T, N>> operator )
  {
    underlying.replaceAll(operator);
  }

  @Override
  public void sort( Comparator<? super TreeItemProvider<T, N>> c )
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
    return (o instanceof MultiTreeItemProvider) &&
      underlying.equals(((MultiTreeItemProvider<?,?>) o).underlying);
  }

  @Override
  public int hashCode()
  {
    return underlying.hashCode();
  }

  @Override
  public TreeItemProvider<T, N> get( int index )
  {
    return underlying.get(index);
  }

  @Override
  public TreeItemProvider<T, N> set( int index,
    TreeItemProvider<T, N> element )
  {
    return underlying.set(index, element);
  }

  @Override
  public void add( int index,
    TreeItemProvider<T, N> element )
  {
    underlying.add(index, element);
  }

  @Override
  public TreeItemProvider<T, N> remove( int index )
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
  public ListIterator<TreeItemProvider<T, N>> listIterator()
  {
    return underlying.listIterator();
  }

  @Override
  public ListIterator<TreeItemProvider<T, N>> listIterator( int index )
  {
    return underlying.listIterator(index);
  }

  @Override
  public List<TreeItemProvider<T, N>> subList( int fromIndex, int toIndex )
  {
    return underlying.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<TreeItemProvider<T, N>> spliterator()
  {
    return underlying.spliterator();
  }

  @Override
  public boolean removeIf(
    Predicate<? super TreeItemProvider<T, N>> filter )
  {
    return underlying.removeIf(filter);
  }

  @Override
  public Stream<TreeItemProvider<T, N>> stream()
  {
    return underlying.stream();
  }

  @Override
  public Stream<TreeItemProvider<T, N>> parallelStream()
  {
    return underlying.parallelStream();
  }

  @Override
  public void forEach(
    Consumer<? super TreeItemProvider<T, N>> action )
  {
    underlying.forEach(action);
  }
}
