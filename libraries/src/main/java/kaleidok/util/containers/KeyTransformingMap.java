package kaleidok.util.containers;

import org.apache.commons.lang3.tuple.Pair;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


public abstract class KeyTransformingMap<K, V> implements Map<K, V>
{
  protected final Map<K, V> underlying;


  protected KeyTransformingMap( Map<K, V> underlying )
  {
    this.underlying = Objects.requireNonNull(underlying);
  }


  protected abstract Object transformUntypedKey( Object key );


  protected abstract K transformTypedKey( K key );


  protected boolean checkUnderlyingMapIntegrity(
    Map<? extends K, ? extends V> m )
  {
    return m.keySet().parallelStream().allMatch(
      (k) -> Objects.equals(k, transformTypedKey(k)));
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
  public boolean containsKey( Object key )
  {
    return underlying.containsKey(transformUntypedKey(key));
  }


  @Override
  public boolean containsValue( Object value )
  {
    return underlying.containsValue(value);
  }


  @Override
  public V get( Object key )
  {
    return underlying.get(transformUntypedKey(key));
  }


  @Override
  public V put( K key, V value )
  {
    return underlying.put(transformTypedKey(key), value);
  }


  @Override
  public V remove( Object key )
  {
    return underlying.remove(transformUntypedKey(key));
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void putAll( Map<? extends K, ? extends V> m )
  {
    if (m.isEmpty())
      return;

    if (hasKeyConflict(m))
    {
      throw new IllegalArgumentException(
        "Map contains conflicting key-value-pairs");
    }

    underlying.putAll(
      m.entrySet().stream().collect(Collectors.toMap(
        ((Function<K, K>) this::transformTypedKey).compose(Entry::getKey),
        Entry::getValue, (a, b) -> b)));
  }


  public boolean hasKeyConflict( final Map<? extends K, ?> m )
  {
    return !m.isEmpty() &&
      m.entrySet().parallelStream().anyMatch((e) -> hasKeyConflict(m, e));
  }


  boolean hasKeyConflict( final Map<? extends K, ?> m,
    Entry<? extends K, ?> e )
  {
    K originalKey = e.getKey(),
      transformedKey = transformTypedKey(originalKey);
    return !Objects.equals(originalKey, transformedKey) &&
        m.containsKey(transformedKey) &&
        !Objects.equals(e.getValue(), m.get(transformedKey));
  }


  <U> Collection<Pair<Entry<K, U>, Entry<K, U>>> findKeyConflicts(
    final Map<K, U> m )
  {
    return m.entrySet().stream()
      .filter((e) -> hasKeyConflict(m, e))
      .map((e) -> {
          K transformedKey = transformTypedKey(e.getKey());
          return Pair.of(
            e,
            (Entry<K, U>) new SimpleImmutableEntry<>(
              transformedKey, m.get(transformedKey)));
        })
      .collect(Collectors.toList());
  }


  @Override
  public void clear()
  {
    underlying.clear();
  }


  @Override
  public Set<K> keySet()
  {
    return underlying.keySet();
  }


  @Override
  public Collection<V> values()
  {
    return underlying.values();
  }


  @Override
  public Set<Entry<K, V>> entrySet()
  {
    return underlying.entrySet();
  }


  @Override
  public boolean equals( Object o )
  {
    return (o instanceof KeyTransformingMap) &&
      this.underlying.equals(((KeyTransformingMap<?, ?>) o).underlying);
  }


  @Override
  public int hashCode()
  {
    return underlying.hashCode() ^ 0xbfaae6bd;
  }


  @Override
  public V getOrDefault( Object key, V defaultValue )
  {
    return underlying.getOrDefault(transformUntypedKey(key), defaultValue);
  }


  @Override
  public void forEach( BiConsumer<? super K, ? super V> action )
  {
    underlying.forEach(action);
  }


  @Override
  public void replaceAll( BiFunction<? super K, ? super V, ? extends V> function )
  {
    underlying.replaceAll(function);
  }


  @Override
  public V putIfAbsent( K key, V value )
  {
    return underlying.putIfAbsent(transformTypedKey(key), value);
  }


  @Override
  public boolean remove( Object key, Object value )
  {
    return underlying.remove(transformUntypedKey(key), value);
  }


  @Override
  public boolean replace( K key, V oldValue, V newValue )
  {
    return underlying.replace(transformTypedKey(key), oldValue, newValue);
  }


  @Override
  public V replace( K key, V value )
  {
    return underlying.replace(transformTypedKey(key), value);
  }


  @Override
  public V computeIfAbsent( K key,
    Function<? super K, ? extends V> mappingFunction )
  {
    return underlying.computeIfAbsent(transformTypedKey(key), mappingFunction);
  }


  @Override
  public V computeIfPresent( K key,
    BiFunction<? super K, ? super V, ? extends V> remappingFunction )
  {
    return underlying.computeIfPresent(transformTypedKey(key), remappingFunction);
  }


  @Override
  public V compute( K key,
    BiFunction<? super K, ? super V, ? extends V> remappingFunction )
  {
    return underlying.compute(transformTypedKey(key), remappingFunction);
  }


  @Override
  public V merge( K key, V value,
    BiFunction<? super V, ? super V, ? extends V> remappingFunction )
  {
    return underlying.merge(transformTypedKey(key), value, remappingFunction);
  }
}
