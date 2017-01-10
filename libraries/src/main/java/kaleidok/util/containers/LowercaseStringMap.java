package kaleidok.util.containers;

import java.util.Locale;
import java.util.Map;


public class LowercaseStringMap<V> extends KeyTransformingMap<String, V>
{
  public final Locale locale;


  public LowercaseStringMap( Map<String, V> underlying )
  {
    this(underlying, null);
  }


  public LowercaseStringMap( Map<String, V> underlying, Locale locale )
  {
    super(underlying);
    this.locale = (locale != null) ? locale : Locale.getDefault();
    assert checkUnderlyingMapIntegrity(underlying);
  }


  @Override
  protected Object transformUntypedKey( Object key )
  {
    return (key instanceof String) ? transformTypedKey((String) key) : key;
  }


  @Override
  protected String transformTypedKey( String key )
  {
    return (key != null) ? key.toLowerCase(locale) : null;
  }


  @Override
  public boolean equals( Object o )
  {
    return
      (o instanceof LowercaseStringMap) &&
      this.locale.equals(((LowercaseStringMap<?>) o).locale) &&
      super.equals(o);
  }


  @Override
  public int hashCode()
  {
    return super.hashCode() ^ locale.hashCode();
  }
}
