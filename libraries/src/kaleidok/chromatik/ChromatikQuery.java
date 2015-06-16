package kaleidok.chromatik;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kaleidok.chromatik.data.ChromatikColor;
import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.chromatik.data.ChromatikResponseDeserializer;
import kaleidok.http.JsonHttpConnection;
import kaleidok.util.Strings;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static kaleidok.http.URLEncoding.appendEncoded;


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
  public URL baseUrl;

  /**
   * Search option map. Possible option keys include "saturation", "darkness",
   * and "rights".
   *
   * To add a color to a search query, construct a {@link ChromatikColor}
   * object and use it as the key to the option entry. The value is the
   * weight of that color as a {@link java.lang.Number} object between 0 and 1.
   */
  public Map<Object, Object> opts = new HashMap<>();

  private StringBuilder sb = new StringBuilder(INITIAL_BUFFER_CAPACITY);

  private static final Gson gson = new GsonBuilder()
    .registerTypeAdapter(
      ChromatikResponse.class, ChromatikResponseDeserializer.INSTANCE)
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
    this.baseUrl = DEFAULT_URL;

    if (colors != null && colors.length != 0)
    {
      Float weight = Math.min(1.f / colors.length, MAX_COLOR_WEIGHT);
      for (int c: colors)
        opts.put(new ChromatikColor(c), weight);
    }
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
      return new URL(baseUrl, getQueryString());
    } catch (MalformedURLException ex) {
      throw new AssertionError(ex);
    }
  }

  /**
   * Builds and returns just the query part of the URL string of this query.
   *
   * @return The current query string
   */
  public String getQueryString()
  {
    return buildQueryString().toString();
  }

  private StringBuilder buildQueryString()
  {
    sb.setLength(0);
    sb.append(QUERY_PATHDELIM)
      .append(QUERY_START).append(QUERY_NAMEDELIM).append(start)
      .append(QUERY_PARAMDELIM)
      .append(QUERY_NHITS).append(QUERY_NAMEDELIM).append(nhits);

    if (!keywords.isEmpty() || !opts.isEmpty())
    {
      sb.append(QUERY_PARAMDELIM).append(QUERY_QUERY)
        .append(QUERY_NAMEDELIM);
      appendEncoded(keywords, sb);

      if (!opts.isEmpty())
      {
        if (!keywords.isEmpty())
          sb.append(QUERY_SPACE);

        sb.append('(').append(QUERY_OPT);
        int totalWeight = 0;
        char[] hexStrBuf = new char[6];
        for (Map.Entry<Object, Object> o: opts.entrySet())
        {
          sb.append(QUERY_SPACE);
          if (o.getKey() instanceof ChromatikColor)
          {
            ChromatikColor c = (ChromatikColor) o.getKey();
            int weight = (int)(((Number) o.getValue()).floatValue() * 100);
            if (weight <= 0 || weight > 100)
              throw new IllegalArgumentException("Color weight lies outside (0, 100]: " + weight);
            totalWeight += weight;

            sb.append(QUERY_OPT_COLOR).append(QUERY_OPT_NAMEDELIM)
              .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
              .append(Strings.toHex(c.value, hexStrBuf))
              .append(QUERY_OPT_VALUEDELIM).append(weight)
              .append(QUERY_OPT_COLOR_SUFFIX).append(QUERY_SPACE)
              .append(QUERY_OPT_COLORGROUP).append(QUERY_OPT_NAMEDELIM)
              .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
              .append(weight);
          }
          else
          {
            appendEncoded(o.getKey().toString(), sb).append(QUERY_OPT_NAMEDELIM);
            appendEncoded(o.getValue().toString(), sb);
          }
        }
        sb.append(')');

        if (totalWeight > 100)
          throw new IllegalArgumentException("Total color weight exceeds 100: " + totalWeight);
      }
    }

    return sb;
  }

  /**
   * Returns the whole URL string of the current query.
   *
   * @return Query URL string
   */
  @Override
  public String toString()
  {
    return baseUrl.toString() + buildQueryString();
  }

  private static final String
    QUERY_START = "start",
    QUERY_NHITS = "nhits",
    QUERY_QUERY = "q",
    QUERY_OPT = "OPT",
    QUERY_OPT_NAMEDELIM = "%3A", // ':'
    QUERY_OPT_VALUEDELIM = "%2F", // '/'
    QUERY_OPT_COLOR_SUFFIX = "%7Bs=200000%7D";

  private static final char
    QUERY_PATHDELIM = '?',
    QUERY_PARAMDELIM = '&',
    QUERY_NAMEDELIM = '=',
    QUERY_SPACE = '+';

  public static final URL DEFAULT_URL;
  static {
    try {
      DEFAULT_URL = new URL("http", "chromatik.labs.exalead.com", "/searchphotos");
    } catch (MalformedURLException ex) {
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
