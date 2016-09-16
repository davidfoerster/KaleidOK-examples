package kaleidok.util.containers;

import kaleidok.util.function.InstanceSupplier;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.*;
import java.util.stream.Collectors;


public class LowercaseStringMap<V> implements Map<String, V>
{
  protected final Map<String, V> underlying;

  public final Locale locale;


  public LowercaseStringMap( Map<String, V> underlying )
  {
    this(underlying, null);
  }


  public LowercaseStringMap( Map<String, V> underlying, Locale locale )
  {
    assert checkMap(underlying);
    this.underlying = Objects.requireNonNull(underlying);
    this.locale = (locale != null) ? locale : Locale.getDefault();
  }


  private static boolean checkKey( String s )
  {
    return s == null || s.isEmpty() || StringUtils.isAllLowerCase(s);
  }


  private static boolean checkMap( Map<String, ?> m )
  {
    return m.keySet().parallelStream().allMatch(LowercaseStringMap::checkKey);
  }


  private Object transformKey( Object key )
  {
    return (key instanceof String) ? transformKey((String) key) : key;
  }


  private String transformKey( String key )
  {
    return (key != null) ? key.toLowerCase(locale) : null;
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
    return underlying.containsKey(transformKey(key));
  }


  @Override
  public boolean containsValue( Object value )
  {
    return underlying.containsValue(value);
  }


  @Override
  public V get( Object key )
  {
    return underlying.get(transformKey(key));
  }


  @Override
  public V put( String key, V value )
  {
    return underlying.put(transformKey(key), value);
  }


  @Override
  public V remove( Object key )
  {
    return underlying.remove(transformKey(key));
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void putAll( Map<? extends String, ? extends V> m )
  {
    if (m.isEmpty())
      return;

    Function<Entry<? extends String, ? extends V>, String> keyMapper =
      ((Function<String, String>) this::transformKey).compose(Entry::getKey);
    Function<Entry<? extends String, ? extends V>, V> valueMapper =
      Entry::getValue;
    BinaryOperator<V> mergeFunction = (a, b) -> a;
    Supplier<?> mapSupplier = new InstanceSupplier<>(underlying);

    if (underlying instanceof ConcurrentMap)
    {
      //noinspection unchecked
      m.entrySet().parallelStream().collect(
        Collectors.toConcurrentMap(
          keyMapper, valueMapper, mergeFunction,
          (Supplier<ConcurrentMap<String,V>>) mapSupplier));
    }
    else
    {
      //noinspection unchecked
      m.entrySet().stream().collect(
        Collectors.toMap(
          keyMapper, valueMapper, mergeFunction,
          (Supplier<Map<String,V>>) mapSupplier));
    }
  }


  @Override
  public void clear()
  {
    underlying.clear();
  }


  @Override
  public Set<String> keySet()
  {
    return underlying.keySet();
  }


  @Override
  public Collection<V> values()
  {
    return underlying.values();
  }


  @Override
  public Set<Entry<String, V>> entrySet()
  {
    return underlying.entrySet();
  }


  @Override
  public boolean equals( Object o )
  {
    return (o instanceof LowercaseStringMap) && underlying.equals(o);
  }


  @Override
  public int hashCode()
  {
    return underlying.hashCode();
  }


  @Override
  public V getOrDefault( Object key, V defaultValue )
  {
    return underlying.getOrDefault(transformKey(key), defaultValue);
  }


  @Override
  public void forEach( BiConsumer<? super String, ? super V> action )
  {
    underlying.forEach(action);
  }


  @Override
  public void replaceAll( BiFunction<? super String, ? super V, ? extends V> function )
  {
    underlying.replaceAll(function);
  }


  @Override
  public V putIfAbsent( String key, V value )
  {
    return underlying.putIfAbsent(transformKey(key), value);
  }


  @Override
  public boolean remove( Object key, Object value )
  {
    return underlying.remove(transformKey(key), value);
  }


  @Override
  public boolean replace( String key, V oldValue, V newValue )
  {
    return underlying.replace(transformKey(key), oldValue, newValue);
  }


  @Override
  public V replace( String key, V value )
  {
    return underlying.replace(transformKey(key), value);
  }


  @Override
  public V computeIfAbsent( String key,
    Function<? super String, ? extends V> mappingFunction )
  {
    return underlying.computeIfAbsent(transformKey(key), mappingFunction);
  }


  @Override
  public V computeIfPresent( String key,
    BiFunction<? super String, ? super V, ? extends V> remappingFunction )
  {
    return underlying.computeIfPresent(transformKey(key), remappingFunction);
  }


  @Override
  public V compute( String key,
    BiFunction<? super String, ? super V, ? extends V> remappingFunction )
  {
    return underlying.compute(transformKey(key), remappingFunction);
  }


  @Override
  public V merge( String key, V value,
    BiFunction<? super V, ? super V, ? extends V> remappingFunction )
  {
    return underlying.merge(transformKey(key), value, remappingFunction);
  }
}
