package http;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


public class MimeTypeMap extends HashMap<String, Float>
{
  public static final String WILDCARD = "*/*";

  public static final Float
    ZERO = Float.valueOf(0),
    ONE = Float.valueOf(1);

  private boolean frozen = false;

  public MimeTypeMap() { }

  public MimeTypeMap( boolean allowAll ) {
    if (allowAll)
      put(WILDCARD, null);
  }

  public MimeTypeMap( String mime, Float q, boolean allowAll ) {
    this(allowAll);
    put(mime, q);
  }

  @Override
  public Float put( String key, Float value )
  {
    checkFrozen();

    if (!MIME_TYPE_PATTERN.matcher(key).matches())
      throw new IllegalArgumentException("Invalid MIME type: " + key);

    if (value != null && !(value.floatValue() >= 0 && value.floatValue() <= 1))
    {
      throw new IllegalArgumentException(
        "Invalid preference qualifier for MIME type " + key + ':' + ' ' +
          value.floatValue());
    }

    return super.put(key, value);
  }

  @Override
  public Float get( Object key )
  {
    Float q = super.get(key);
    return (q != null || !containsKey(key)) ? q : ONE;
  }

  public String allows( String mime ) {
    return allows(mime, true);
  }

  public String allows( String mime, boolean wildcards )
  {
    Float q = get(mime);
    if (q != null)
      return (q.floatValue() > 0) ? mime : null;
    if (!wildcards || WILDCARD.equals(mime))
      return null;

    int p = mime.indexOf('/');
    if (p < 0)
      throw new IllegalArgumentException("Invalid MIME type: " + mime);
    assert mime.indexOf('/', p + 1) < 0;
    if (mime.length() != p + 2 || mime.charAt(p + 1) != '*') {
      char[] buf = new char[p + 2];
      mime.getChars(0, p, buf, 0);
      buf[p] = '/';
      buf[p + 1] = '*';
      mime = new String(buf);
      q = get(mime);
      if (q != null)
        return (q.floatValue() > 0) ? mime : null;
    }

    q = get(WILDCARD);
    return (q != null && q.floatValue() > 0) ? WILDCARD : null;
  }

  @Override
  public String toString()
  {
    if (isEmpty() || (size() == 1 && ONE.equals(get(WILDCARD))))
      return "";

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Float> e: entrySet()) {
      assert MIME_TYPE_PATTERN.matcher(e.getKey()).matches();

      if (sb.length() != 0)
        sb.append(',');
      sb.append(e.getKey());

      Float q = e.getValue();
      if (q != null && q < 1) {
        assert q >= 0 && q <= 1;
        String qs = qFormat.format(q.doubleValue());
        assert qs.length() <= 1 || "0.,".indexOf(qs.charAt(qs.length() - 1)) < 0;
        sb.append(";q=").append(qs);
      }
    }
    return (sb.length() != 0) ? sb.toString() : "";
  }

  public boolean isFrozen()
  {
    return frozen;
  }

  public void freeze()
  {
    if (!frozen) {
      checkFrozen();
    } else {
      this.frozen = frozen;
    }
  }

  private void checkFrozen() {
    if (frozen)
      throw new UnsupportedOperationException("frozen");
  }

  @Override
  public Float remove( Object key )
  {
    checkFrozen();
    return super.remove(key);
  }

  public static final Pattern MIME_TYPE_PATTERN = Pattern.compile(
    "^(?:\\*/\\*|[a-z-]+/(\\*|[a-z-]+))$");

  private static final NumberFormat qFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
  static {
    qFormat.setMinimumIntegerDigits(1);
    qFormat.setMaximumIntegerDigits(1);
    qFormat.setMinimumFractionDigits(0);
    qFormat.setMaximumFractionDigits(3);
  };
}
