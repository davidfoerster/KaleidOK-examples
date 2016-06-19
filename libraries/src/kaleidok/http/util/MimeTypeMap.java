package kaleidok.http.util;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Represents a set of MIME types and their acceptance rating as used in HTTP
 * Accept headers.
 */
public class MimeTypeMap extends HashMap<String, Float>
{
  private static final long serialVersionUID = -6412623527769040820L;

  public static final char WILDCARD_CHAR = '*';

  /**
   * Wildcard string for any MIME type
   */
  public static final String WILDCARD = WILDCARD_CHAR + "/" + WILDCARD_CHAR;

  /**
   * Wrapped float value 0 (because of its frequent occurrence)
   */
  public static final Float ZERO = 0.f;

  /**
   * Wrapped float value 1 (because of its frequent occurrence)
   */
  public static final Float ONE = 1.f;


  private boolean frozen = false;

  private String acceptString = null;


  /**
   * Constructs an empty MIME type set.
   */
  public MimeTypeMap() { }

  public MimeTypeMap( int initialCapacity )
  {
    super(initialCapacity);
  }


  /**
   * Add the wildcard MIME entry to this map.
   *
   * @return see {@link #put(String, Float)}
   */
  public Float allowAll()
  {
    return put(WILDCARD, null);
  }


  /**
   * Adds or replaces a MIME type with a given preference rating to this set.
   *
   * @param key  A MIME type string
   * @param value  A preference rating; {@code null} is the default
   * @return  The rating previously associated with this MIME type (may be
   *   {@code null})
   * @see Map#put(Object, Object)
   * @throws UnsupportedOperationException if this map is frozen.
   * @throws IllegalArgumentException if the MIME type string is
   *   invalid or the preference rating is not between 0 and 1.
   */
  @Override
  public Float put( String key, Float value )
  {
    checkFrozen();

    if (!MIME_TYPE_PATTERN.matcher(key).matches())
      throw new IllegalArgumentException("Invalid MIME type: " + key);

    if (value != null && !(value >= 0 && value <= 1))
    {
      throw new IllegalArgumentException(
        "Invalid preference qualifier for MIME type " + key + ':' + ' ' +
          value);
    }

    return super.put(key, value);
  }

  /**
   * Returns the currently associated preference rating for a MIME type or 1,
   * if it has the default rating.
   *
   * @param key  A MIME type string
   * @return  Associated preference rating
   */
  @Override
  public Float get( Object key )
  {
    Float q = super.get(key);
    return (q != null || !containsKey(key)) ? q : ONE;
  }

  /**
   * Equivalent to {@link #allows(String, boolean)} with allowed wildcards.
   *
   * @param mime  A MIME type
   * @return  Matching entry key that allowed {@code mime}.
   * @see #allows(String, boolean)
   */
  public String allows( String mime ) {
    return allows(mime, true);
  }

  /**
   * Checks whether a given MIME type string is allowed by this MIME type set.
   * That includes wildcards and excludes zero preference ratings.
   *
   * @param mime  A MIME type string
   * @param wildcards  Whether to check for wildcard entries
   * @return  The MIME type or wildcard string under which {@code mime} is
   *   allowed or {@code null} if it isn't allowed.
   */
  public String allows( String mime, boolean wildcards )
  {
    assert mime == null || MIME_TYPE_PATTERN.matcher(mime).matches() :
      getMismatchError(mime);

    Float q = get(mime);
    if (q != null)
      return (q > 0) ? mime : null;
    if (!wildcards || WILDCARD.equals(mime))
      return null;

    if (mime != null)
    {
      int p = mime.indexOf('/') + 1;
      assert p > 0 && p < mime.length() :
        "'/' doesn't occur before the last character of \"" + mime + '\"';
      if (mime.charAt(p) != WILDCARD_CHAR)
      {
        StringBuilder buf = new StringBuilder(p + 1);
        buf.append(mime, 0, p).append(WILDCARD_CHAR);
        mime = buf.toString();
        q = get(mime);
        if (q != null)
          return (q > 0) ? mime : null;
      }
    }

    mime = WILDCARD;
    q = get(mime);
    return (q != null && q > 0) ? mime : null;
  }

  /**
   * Returns a string representation of this MIME type set as one would use it
   * in an HTTP Accept header entry.
   *
   * @return  A MIME type acceptance string
   */
  @Override
  public String toString()
  {
    if (isEmpty() || (size() == 1 && ONE.equals(get(WILDCARD))))
      return "";

    if (frozen && this.acceptString != null)
      return this.acceptString;

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Float> e: entrySet()) {
      assert MIME_TYPE_PATTERN.matcher(e.getKey()).matches() :
        getMismatchError(e.getKey());

      if (sb.length() != 0)
        sb.append(',');
      sb.append(e.getKey());

      Float q = e.getValue();
      if (q != null && q < 1) {
        assert q >= 0 && q <= 1 : q + " lies outside of [0, 1]";
        String qs = qFormat.format(q.doubleValue());
        assert qs.length() <= 1 || "0.,".indexOf(qs.charAt(qs.length() - 1)) < 0 :
          '\"' + qs + "\" is longer than 1 and ends in any of '0', ',', or '.'";
        sb.append(";q=").append(qs);
      }
    }
    String acceptString = (sb.length() != 0) ? sb.toString() : "";
    if (frozen)
      this.acceptString = acceptString;
    return acceptString;
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

  private void checkFrozen() {
    if (frozen)
      throw new UnsupportedOperationException("frozen");
  }

  /**
   * @see Map#remove(Object)
   * @param key  A key top remove
   * @return  The previously associated preference rating
   * @throws UnsupportedOperationException if this set is frozen.
   */
  @Override
  public Float remove( Object key )
  {
    checkFrozen();
    return super.remove(key);
  }

  public static final Pattern MIME_TYPE_PATTERN = Pattern.compile(
    "^(?:\\*/\\*|[a-z.-]+/(?:\\*|[a-z.-]+))$");

  private static final NumberFormat qFormat =
    NumberFormat.getNumberInstance(Locale.ROOT);
  static {
    qFormat.setMinimumIntegerDigits(1);
    qFormat.setMaximumIntegerDigits(1);
    qFormat.setMinimumFractionDigits(0);
    qFormat.setMaximumFractionDigits(3);
  }


  private static String getMismatchError( String mime )
  {
    return String.format("\"%s\" isn't a MIME type matching \"%s\"",
      mime, MIME_TYPE_PATTERN.pattern());
  }
}
