package kaleidok.chromatik;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.http.JsonHttpConnection;
import kaleidok.util.Strings;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;


/**
 * Holds all the parameters to build a Chromatik image search query.
 */
public class ChromatikQuery
{
  /**
   * The start index of the requested section of the result set
   */
  public int start = 0;

  /**
   * Maximum result set section size
   */
  public int nhits;

  /**
   * Search keywords (if any) separated by spaces
   */
  public String keywords;

  /**
   * The protocol, host, and path component of the query URL
   */
  public URI baseUri;

  /**
   * Search option map. Possible option keys include "saturation", "darkness",
   * and "rights".
   *
   * To add a color to a search query, construct a {@link ChromatikColor}
   * object and use it as the key to the option entry. The value is the
   * weight of that color as a {@link java.lang.Number} object between 0 and 1.
   */
  public Map<Object, Object> opts = new HashMap<>();


  protected static final Gson gson = new GsonBuilder()
    .registerTypeAdapter(
      ChromatikResponse.class, ChromatikResponse.Deserializer.INSTANCE)
    .excludeFieldsWithoutExposeAnnotation()
    .create();


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
   * @param nhits  Result set size
   * @param keywords  Query keywords
   * @param colors  RGB color values to search for; the weight is the inverse
   *   of the amount of colors
   */
  public ChromatikQuery( int nhits, String keywords, int... colors )
  {
    this.nhits = nhits;
    this.keywords = (keywords != null) ? keywords : "";
    this.baseUri = DEFAULT_URI;

    if (colors != null && colors.length != 0)
    {
      Float weight = Math.min(1.f / colors.length, MAX_COLOR_WEIGHT);
      for (int c: colors)
        opts.put(new ChromatikColor(c), weight);
    }
  }


  public ChromatikQuery( ChromatikQuery other )
  {
    start = other.start;
    nhits = other.nhits;
    keywords = other.keywords;
    baseUri = other.baseUri;
    opts = new HashMap<>(other.opts);
  }


  /**
   * Issues the query as specified and returns the result object.
   * @return  Query result
   */
  public ChromatikResponse getResult() throws IOException
  {
    URL url = getUrl();
    //System.err.println(url.toString());
    return fetch(url);
  }

  protected static ChromatikResponse fetch( URL url ) throws IOException
  {
    return JsonHttpConnection.openURL(url)
      .get(ChromatikResponse.class, gson);
  }


  public Callable<ChromatikResponse> asCallable()
  {
    return new ChromatikResponseCallable(getUrl());
  }

  private static class ChromatikResponseCallable implements Callable<ChromatikResponse>
  {
    private final URL url;

    public ChromatikResponseCallable( URL url )
    {
      this.url = url;
    }

    @Override
    public ChromatikResponse call() throws IOException
    {
      return fetch(url);
    }
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
    URIBuilder ub = new URIBuilder(baseUri)
      .addParameter(QUERY_START, Integer.toString(start))
      .addParameter(QUERY_NHITS, Integer.toString(nhits));

    String searchQuery = keywords;
    if (!opts.isEmpty())
    {
      StringBuilder sb = new StringBuilder();
      if (!searchQuery.isEmpty())
        sb.append(searchQuery).append(' ');

      sb.append('(').append("OPT");
      int totalWeight = 0;
      char[] hexStrBuf = new char[6];
      for (Map.Entry<Object, Object> o: opts.entrySet())
      {
        sb.append(' ');
        if (o.getKey() instanceof ChromatikColor)
        {
          ChromatikColor c = (ChromatikColor) o.getKey();
          int weight = (int)(((Number) o.getValue()).floatValue() * 100);
          if (weight <= 0 || weight > 100) {
            throw new IllegalArgumentException(
              "Color weight lies outside of (0, 100]: " + weight);
          }
          totalWeight += weight;

          sb.append(QUERY_OPT_COLOR).append(QUERY_OPT_NAMEDELIM)
            .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
            .append(Strings.toHex(c.value, hexStrBuf))
            .append(QUERY_OPT_VALUEDELIM).append(weight)
            .append("{s=200000}").append(' ')
            .append(QUERY_OPT_COLORGROUP).append(QUERY_OPT_NAMEDELIM)
            .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
            .append(weight);
        }
        else
        {
          CharSequence key = toCharSequence(o.getKey()),
            value = toCharSequence(o.getValue());
          assert (key != null && key.toString().indexOf(QUERY_OPT_NAMEDELIM) < 0) &&
            (value == null || value.toString().indexOf(QUERY_OPT_NAMEDELIM) < 0);

          sb.append(key);
          if (value != null)
            sb.append(QUERY_OPT_NAMEDELIM).append(value);
        }
      }
      sb.append(')');
      searchQuery = sb.toString();

      if (totalWeight > 100)
        throw new IllegalArgumentException("Total color weight exceeds 100: " + totalWeight);
    }

    if (!searchQuery.isEmpty())
      ub.addParameter(QUERY_QUERY, searchQuery);

    return ub;
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


  private static final String
    QUERY_START = "start",
    QUERY_NHITS = "nhits",
    QUERY_QUERY = "q";

  private static final char
    QUERY_OPT_NAMEDELIM = ':',
    QUERY_OPT_VALUEDELIM = '/';


  public static final URI DEFAULT_URI;
  static {
    try {
      DEFAULT_URI =
        new URI("http", "chromatik.labs.exalead.com", "/searchphotos", null);
    } catch (URISyntaxException ex) {
      throw new AssertionError(ex);
    }
  }


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


  public static final int QUERY_NHITS_DEFAULT = 40;

  public static final float MAX_COLOR_WEIGHT = 0.25f;

  public static int INITIAL_BUFFER_CAPACITY = 128;
}
