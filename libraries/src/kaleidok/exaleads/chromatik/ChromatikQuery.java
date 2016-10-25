package kaleidok.exaleads.chromatik;

import kaleidok.exaleads.chromatik.data.ChromatikColor;
import kaleidok.exaleads.chromatik.data.ChromatikResponse;
import kaleidok.http.JsonHttpConnection;
import kaleidok.google.gson.TypeAdapterManager;
import kaleidok.util.Objects;
import kaleidok.util.Strings;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;


/**
 * Holds all the parameters to build a Chromatik image search query.
 */
public class ChromatikQuery implements Serializable, Cloneable
{
  private static final long serialVersionUID = -4020430402877695173L;

  /**
   * The start index of the requested section of the result set
   */
  private int start = 0;

  /**
   * Maximum result set section size
   */
  private int nHits;

  /**
   * Search keywords (if any) separated by spaces
   */
  private String keywords;

  /**
   * The protocol, host, and path component of the query URL
   */
  private URI baseUri;

  /**
   * Search option map. Possible option keys include "saturation", "darkness",
   * and "rights".
   *
   * To add a color to a search query, construct a {@link ChromatikColor}
   * object and use it as the key to the option entry. The value is the
   * weight of that color as a {@link Number} object between 0 and 1.
   */
  public Map<Serializable, Serializable> opts = new HashMap<>();


  static {
    TypeAdapterManager.registerTypeAdapter(
      ChromatikResponse.class, ChromatikResponse.Deserializer.INSTANCE);
  }


  /**
   * Constructs a query object with the default result set size and no
   * keywords.
   */
  public ChromatikQuery()
  {
    this(QUERY_NHITS_DEFAULT, null, (int[]) null);
  }


  /**
   * Constructs a query object with preset parameters.
   *
   * @param nHits  Result set size
   * @param keywords  Query keywords
   * @param colors  RGB color values to search for; the weight is the inverse
   *   of the amount of colors
   */
  public ChromatikQuery( int nHits, String keywords, int... colors )
  {
    this.nHits = nHits;
    this.keywords = (keywords != null) ? keywords : "";
    this.baseUri = DEFAULT_URI;

    if (colors != null && colors.length != 0)
    {
      Double weight = Math.min(1.0 / colors.length, MAX_COLOR_WEIGHT);
      for (int c: colors)
        opts.put(new ChromatikColor(c), weight);
    }
  }


  @Override
  public boolean equals( Object o )
  {
    if (o == this)
      return true;
    if (!(o instanceof ChromatikQuery))
      return false;

    ChromatikQuery ocq = (ChromatikQuery) o;
    return getStart() == ocq.getStart() && getNHits() == ocq.getNHits() &&
      java.util.Objects.equals(getKeywords(), ocq.getKeywords()) &&
      java.util.Objects.equals(getBaseUri(), ocq.getBaseUri()) &&
      opts.equals(ocq.opts);
  }


  @Override
  public int hashCode()
  {
    return Objects.hashCode(Objects.hashCode(Objects.hashCode(Objects.hashCode(
      Integer.hashCode(getStart()), getNHits()), getKeywords()), getBaseUri()),
      opts);
  }


  @SuppressWarnings({ "unchecked", "CloneCallsConstructors" })
  @Override
  public ChromatikQuery clone()
  {
    ChromatikQuery other;
    try
    {
      other = (ChromatikQuery) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      throw new InternalError(ex);
    }
    if (other.opts != null)
    {
      Map<Serializable, Serializable> optsClone = null;
      if (other.opts instanceof Cloneable) try
      {
        //noinspection OverlyStrongTypeCast
        optsClone = Objects.clone(
          (Cloneable & Map<Serializable, Serializable>) other.opts);
      }
      catch (CloneNotSupportedException ignored)
      {
        // leave the null reference to invoke the fall-back behaviour
      }
      other.opts = (optsClone != null) ? optsClone : new HashMap<>(other.opts);
    }
    return other;
  }


  /**
   * Issues the query as specified and returns the result object.
   * @return  Query result
   */
  public ChromatikResponse getResult() throws IOException
  {
    return fetch(getUrl());
  }

  protected static ChromatikResponse fetch( URL url ) throws IOException
  {
    try (JsonHttpConnection con = JsonHttpConnection.openURL(url))
    {
      return con.get(ChromatikResponse.class, TypeAdapterManager.getGson());
    }
  }


  public Callable<ChromatikResponse> asCallable()
  {
    final URL url = getUrl();
    return () -> fetch(url);
  }


  /**
   * Returns a URL object with the current service specification and query
   * parameters.
   *
   * @return  Query URL
   */
  public URL getUrl()
  {
    try {
      return new URL(buildUri().toString());
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }

  public URI getUri()
  {
    try {
      return buildUri().build();
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


  private URIBuilder buildUri()
  {
    URIBuilder ub = new URIBuilder(getBaseUri())
      .addParameter(QUERY_START, Integer.toString(getStart()))
      .addParameter(QUERY_NHITS, Integer.toString(getNHits()));

    String searchQuery = getKeywords();
    if (!opts.isEmpty())
    {
      StringBuilder sb = new StringBuilder(1 << 8);
      sb.append(searchQuery);
      buildColorSubquery(sb);

      for (Map.Entry<?, ?> o: opts.entrySet())
      {
        if (!(o.getKey() instanceof ChromatikColor))
        {
          CharSequence key = toCharSequence(o.getKey()),
            value = toCharSequence(o.getValue());
          assert key != null && assertValidChars(key, QUERY_OPT_NAMEDELIM);
          assert value == null || assertValidChars(value, QUERY_OPT_NAMEDELIM);

          sb.append(' ').append(key);
          if (value != null)
            sb.append(QUERY_OPT_NAMEDELIM).append(value);
        }
      }

      searchQuery = sb.toString();
    }

    if (!searchQuery.isEmpty())
      ub.addParameter(QUERY_QUERY, searchQuery);

    return ub;
  }


  private static boolean assertValidChars( CharSequence s, char c )
  {
    assert s.toString().indexOf(c) < 0 :
      String.format("\"%s\" contains delimiter character '%c'", s, c);
    return true;
  }


  private StringBuilder buildColorSubquery( StringBuilder sb )
  {
    Map<ChromatikColor, Number> colors = opts.entrySet().stream()
      .filter((o) -> o.getKey() instanceof ChromatikColor)
      .collect(Collectors.toMap(
        (o) -> (ChromatikColor) o.getKey(),
        (o) -> (Number) requireNonNull(o.getValue())));

    colors.values().stream()
      .mapToDouble(Number::doubleValue)
      .filter((w) -> w != w || w <= 0 || w > 1)
      .findAny().ifPresent((illegalWeight) -> {
          throw new IllegalArgumentException(
            "Color weight lies outside of (0, 1]: " + illegalWeight);
        });

    double totalWeight =
      colors.values().stream().mapToDouble(Number::doubleValue).sum();
    if (totalWeight > 1) {
      throw new IllegalArgumentException(
        "Total color weight exceeds 1: " + totalWeight);
    }

    Map<String, Number> colorGroups = colors.entrySet().stream()
      .collect(Collectors.groupingBy(
        (e) -> e.getKey().groupName,
        Collectors.reducing(null, Map.Entry::getValue, (a, b) ->
          (a == null) ? b :
          (b == null) ? a :
            a.doubleValue() + b.doubleValue())));

    if (!colors.isEmpty())
    {
      if (sb.length() != 0)
        sb.append(' ');
      sb.append('(');

      char[] hexStrBuf = new char[6];
      for (Map.Entry<ChromatikColor, Number> o: colors.entrySet())
      {
        ChromatikColor c = o.getKey();
        int weight = (int) (o.getValue().doubleValue() * 100);
        sb.append("OPT").append(' ')
          .append(QUERY_OPT_COLOR).append(QUERY_OPT_NAMEDELIM)
          .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
          .append(Strings.toHexDigits(c.value, hexStrBuf))
          .append(QUERY_OPT_VALUEDELIM).append(weight)
          .append("{s=200000}").append(' ');
      }

      for (Map.Entry<String, Number> o: colorGroups.entrySet())
      {
        int weight = (int) (o.getValue().doubleValue() * 100);
        sb.append(QUERY_OPT_COLORGROUP).append(QUERY_OPT_NAMEDELIM)
          .append(o.getKey()).append(QUERY_OPT_VALUEDELIM)
          .append(weight).append(' ');
      }

      sb.setCharAt(sb.length() - 1, ')');
    }
    return sb;
  }


  public void randomizeRequestedSubset( int expectedResultCount, Random random )
  {
    int nHits = getNHits();
    if (expectedResultCount > nHits)
      setStart(random.nextInt(expectedResultCount - nHits + 1));
  }


  private static CharSequence toCharSequence( Object o )
  {
    return
      (o == null) ? null :
      (o instanceof CharSequence) ? (CharSequence) o :
        o.toString();
  }


  /**
   * Returns the whole URL string of the current query.
   *
   * @return Query URL string
   */
  @Override
  public String toString()
  {
    return buildUri().toString();
  }


  public int getStart()
  {
    return start;
  }

  public void setStart( int start )
  {
    this.start = start;
  }


  public int getNHits()
  {
    return nHits;
  }

  public void setNHits( int nHits )
  {
    this.nHits = nHits;
  }


  public String getKeywords()
  {
    return keywords;
  }

  public void setKeywords( String keywords )
  {
    this.keywords = (keywords != null) ? keywords : "";
  }


  public URI getBaseUri()
  {
    return baseUri;
  }

  public void setBaseUri( URI baseUri )
  {
    this.baseUri = requireNonNull(baseUri);
  }


  @SuppressWarnings("SpellCheckingInspection")
  private static final String
    QUERY_START = "start",
    QUERY_NHITS = "nhits",
    QUERY_QUERY = "q";

  @SuppressWarnings("SpellCheckingInspection")
  private static final char
    QUERY_OPT_NAMEDELIM = ':',
    QUERY_OPT_VALUEDELIM = '/';


  public static final URI DEFAULT_URI;
  static {
    try {
      //noinspection SpellCheckingInspection
      DEFAULT_URI =
        new URI("http", "chromatik.labs.exalead.com", "/searchphotos", null);
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


  @SuppressWarnings({ "unused", "SpellCheckingInspection" })
  public static final String
    QUERY_OPT_COLOR = "color",
    QUERY_OPT_COLORGROUP = "colorgroup",
    QUERY_OPT_SATURATION = "saturation",
    QUERY_OPT_SATURATION_COLORFUL = "Colorful",
    QUERY_OPT_SATURATION_GRAYSCALE = "Grayscale",
    QUERY_OPT_DARKNESS = "darkness",
    QUERY_OPT_DARKNESS_BRIGHT = "Bright",
    QUERY_OPT_DARKNESS_DARK = "Dark",
    QUERY_OPT_RIGHTS = "rights";


  @SuppressWarnings("SpellCheckingInspection")
  public static final int QUERY_NHITS_DEFAULT = 40;

  public static final float MAX_COLOR_WEIGHT = 0.25f;
}
