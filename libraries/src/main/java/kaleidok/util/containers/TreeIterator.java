package kaleidok.util.containers;

import javafx.scene.control.TreeItem;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;


public class TreeIterator<T> implements Iterator<T>, Iterable<T>
{
  private final Function<? super T, ? extends Iterator<T>> childrenGetter;

  private final Deque<Iterator<T>> stack = new ArrayDeque<>();

  private Iterator<T> currentIterator;


  public static <T extends Iterable<T>> TreeIterator<T> getInstance( T root )
  {
    return new TreeIterator<>(root, Iterable::iterator);
  }


  public static <E> TreeIterator<TreeItem<E>> getInstance( TreeItem<E> root )
  {
    return new TreeIterator<>(root, (node) -> node.getChildren().iterator());
  }


  public TreeIterator( T root,
    Function<? super T, ? extends Iterator<T>> childrenGetter )
  {
    this.childrenGetter = Objects.requireNonNull(childrenGetter);
    currentIterator =
      Collections.singletonList(Objects.requireNonNull(root)).iterator();
    stack.addLast(currentIterator);
  }


  @Override
  public boolean hasNext()
  {
    while (!stack.isEmpty())
    {
      if (stack.getLast().hasNext())
        return true;
      stack.removeLast();
    }
    return false;
  }


  @Override
  public T next()
  {
    Iterator<T> top;
    //noinspection LoopConditionNotUpdatedInsideLoop
    while (!(top = stack.getLast()).hasNext())
      stack.removeLast();

    T next = top.next();
    currentIterator = top;
    if (next != null)
      stack.addLast(Objects.requireNonNull(childrenGetter.apply(next)));

    return next;
  }


  @Override
  public void remove()
  {
    currentIterator.remove();
  }


  @Override
  public Iterator<T> iterator()
  {
    return this;
  }
}
