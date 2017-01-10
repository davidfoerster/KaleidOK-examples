package kaleidok.util.containers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


@SuppressWarnings("TransientFieldInNonSerializableClass")
public class FreezableMap<K, V> implements Map<K, V>
{
  protected final Map<K, V> underlying;

  private boolean frozen = false;


  /**
   * Protected no-args constructor for deserialization of deriving classes.
   * Don't call this explicitly unless you want an immutable empty map.
   */
  protected FreezableMap()
  {
    underlying = Collections.emptyMap();
  }


  public FreezableMap( Map<K, V> underlying )
  {
    this.underlying = Objects.requireNonNull(underlying);
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
    return underlying.containsKey(key);
  }


  @Override
  public boolean containsValue( Object value )
  {
    return underlying.containsValue(value);
  }


  @Override
  public V get( Object key )
  {
    return underlying.get(key);
  }


  @Override
  public V put( K key, V value )
  {
    checkFrozen();
    checkValidEntry(key, value);
    return underlying.put(key, value);
  }


  protected void checkValidEntry( K key, V value )
  {
    if (!isValidEntry(key, value))
      throw new IllegalArgumentException(key + " -> " + value);
  }


  @SuppressWarnings("UnusedParameters")
  protected boolean isValidEntry( K key, V value )
  {
    return true;
  }


  @Override
  public V remove( Object key )
  {
    checkFrozen();
    return underlying.remove(key);
  }


  @Override
  public void putAll( Map<? extends K, ? extends V> m )
  {
    checkFrozen();
    for (Entry<K, V> e: underlying.entrySet())
      checkValidEntry(e.getKey(), e.getValue());
    underlying.putAll(m);
  }


  @Override
  public V getOrDefault( Object key, V defaultValue )
  {
    return underlying.getOrDefault(key, defaultValue);
  }


  @Override
  public V putIfAbsent( K key, V value )
  {
    checkFrozen();
    checkValidEntry(key, value);
    return underlying.putIfAbsent(key, value);
  }


  @Override
  public boolean remove( Object key, Object value )
  {
    checkFrozen();
    return underlying.remove(key, value);
  }


  @Override
  public boolean replace( K key, V oldValue, V newValue )
  {
    checkFrozen();
    checkValidEntry(key, newValue);
    return underlying.replace(key, oldValue, newValue);
  }


  @Override
  public V replace( K key, V value )
  {
    checkFrozen();
    checkValidEntry(key, value);
    return underlying.replace(key, value);
  }


  @Override
  public void clear()
  {
    checkFrozen();
    underlying.clear();
  }


  private transient Set<K> cachedKeys = null;

  @Override
  public Set<K> keySet()
  {
    if (!frozen)
      return underlying.keySet();
    if (cachedKeys == null)
      cachedKeys = Collections.unmodifiableSet(underlying.keySet());
    return cachedKeys;
  }


  private transient Collection<V> cachedValues = null;

  @Override
  public Collection<V> values()
  {
    if (!frozen)
      return underlying.values();
    if (cachedValues == null)
      cachedValues = Collections.unmodifiableCollection(underlying.values());
    return cachedValues;
  }


  private transient Set<Entry<K, V>> cachedEntrySet = null;

  @Override
  public Set<Entry<K, V>> entrySet()
  {
    if (!frozen)
      return underlying.entrySet();
    if (cachedEntrySet == null)
      cachedEntrySet = Collections.unmodifiableSet(underlying.entrySet());
    return cachedEntrySet;
  }


  /**
   * @return  Whether this set is froze.
   */
  public boolean isFrozen()
  {
    return frozen;
  }


  /**
   * Freezes this set to prohibit any later modifications.
   */
  public void freeze()
  {
    frozen = true;
  }


  protected void checkFrozen()
  {
    if (frozen)
      throw new UnsupportedOperationException("frozen");
  }


  @Override
  public void forEach( BiConsumer<? super K, ? super V> action )
  {
    underlying.forEach(action);
  }


  @Override
  public V computeIfAbsent( K key, Function<? super K, ? extends V> mappingFunction )
  {
    checkFrozen();
    return underlying.computeIfAbsent(key, mappingFunction);
  }


  @Override
  public V computeIfPresent( K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction )
  {
    checkFrozen();
    return underlying.computeIfPresent(key, remappingFunction);
  }


  @Override
  public V compute( K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction )
  {
    checkFrozen();
    return underlying.compute(key, remappingFunction);
  }
}
