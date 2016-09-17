package kaleidok.util.containers;

import java.util.*;
import java.util.function.BiConsumer;


public class ChainedMap<K, V> implements Map<K, V>
{
  private final Map<K, V> main;

  private Map<K, V> fallback;


  @SuppressWarnings("unchecked")
  public ChainedMap( Map<? extends K, ? extends V> main )
  {
    this(main, null);
  }


  @SuppressWarnings("unchecked")
  public ChainedMap( Map<? extends K, ? extends V> main,
    Map<? extends K, ? extends V> fallback )
  {
    Objects.requireNonNull(main);
    this.main = (Map<K, V>) main;
    this.fallback =
      (fallback != null) ? (Map<K, V>) fallback : Collections.emptyMap();
  }


  public Map<? extends K, ? extends V> getMain()
  {
    return main;
  }


  public Map<? extends K, ? extends V> getFallback()
  {
    return fallback;
  }


  @SuppressWarnings("unchecked")
  public void setFallback( Map<? extends K, ? extends V> fallback )
  {
    this.fallback =
      (fallback != null) ? (Map<K, V>) fallback : Collections.emptyMap();
  }


  @Override
  public int size()
  {
    return main.size();
  }


  @Override
  public boolean isEmpty()
  {
    return main.isEmpty();
  }


  @Override
  public boolean containsKey( Object key )
  {
    return main.containsKey(key) || fallback.containsKey(key);
  }


  @Override
  public boolean containsValue( Object value )
  {
    return main.containsValue(value) || fallback.containsValue(value);
  }


  @SuppressWarnings("SuspiciousMethodCalls")
  @Override
  public V get( Object key )
  {
    V v = main.get(key);
    return (v != null || main.containsKey(key)) ? v : fallback.get(key);
  }


  @Override
  public V put( K key, V value )
  {
    return main.put(key, value);
  }


  @Override
  public V remove( Object key )
  {
    return main.remove(key);
  }


  @Override
  public void putAll( Map<? extends K, ? extends V> m )
  {
    main.putAll(m);
  }


  @Override
  public void clear()
  {
    main.clear();
  }


  @Override
  public Set<K> keySet()
  {
    return main.keySet();
  }


  @Override
  public Collection<V> values()
  {
    return main.values();
  }


  @Override
  public Set<Entry<K, V>> entrySet()
  {
    return main.entrySet();
  }


  @Override
  public boolean equals( Object o )
  {
    if (o == this)
      return true;
    if (!(o instanceof ChainedMap))
      return fallback.isEmpty() && main.equals(o);

    ChainedMap<?,?> ocm = (ChainedMap<?,?>) o;
    return main.equals(ocm.main) && fallback.equals(ocm.fallback);
  }


  @Override
  public int hashCode()
  {
    return main.hashCode() ^ fallback.hashCode();
  }


  @SuppressWarnings("SuspiciousMethodCalls")
  @Override
  public V getOrDefault( Object key, V defaultValue )
  {
    V v = main.get(key);
    return (v != null || main.containsKey(key)) ?
      v :
      fallback.getOrDefault(key, defaultValue);
  }


  @Override
  public void forEach( BiConsumer<? super K, ? super V> action )
  {
    main.forEach(action);
  }
}
