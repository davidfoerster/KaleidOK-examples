package kaleidok.util.function;

@FunctionalInterface
public interface ChangeListener<T, V>
{
  void notifyChange( T owner, V oldValue, V newValue );
}
