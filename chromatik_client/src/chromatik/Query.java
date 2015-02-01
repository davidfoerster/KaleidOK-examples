package chromatik;

import http.JsonConnection;
import processing.data.JSONArray;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static processing.core.PApplet.urlEncode;


public class Query
{
  public int start = 0;

  public int nhits;

  public String keywords;

  public Map<Object, Object> opts = new HashMap<Object, Object>();

  public String urlPath = DEFAULT_URL_PATH;

  private StringBuilder sb = new StringBuilder(INITIAL_BUFFER_CAPACITY);

  public Query()
  {
    this(QUERY_NHITS_DEFAULT, null);
  }

  public Query(int nhits, String keywords, int... colors)
  {
    this.nhits = nhits;
    this.keywords = (keywords != null) ? keywords : "";

    if (colors != null && colors.length != 0)
    {
      Float weight = Math.min(1.f / colors.length, MAX_COLOR_WEIGHT);
      for (int c: colors)
        opts.put(new Color(c), weight);
    }
  }

  public JSONArray getResult()
  {
    try {
      return JsonConnection.openURL(getUrl()).getArray();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public URL getUrl()
  {
    try {
      return new URL(toString());
    } catch (MalformedURLException e) {
      throw new Error(e);
    }
  }

  public String getQueryString()
  {
    return buildQueryString().substring(urlPath.length());
  }

  private StringBuilder buildQueryString()
  {
    sb.setLength(0);
    sb.append(urlPath).append(QUERY_PATHDELIM)
      .append(QUERY_START).append(QUERY_NAMEDELIM).append(start)
      .append(QUERY_PARAMDELIM)
      .append(QUERY_NHITS).append(QUERY_NAMEDELIM).append(nhits);

    if (!keywords.isEmpty() || !opts.isEmpty())
    {
      sb.append(QUERY_PARAMDELIM).append(QUERY_QUERY)
        .append(QUERY_NAMEDELIM).append(urlEncode(keywords));

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
          if (o.getKey() instanceof Color)
          {
            Color c = (Color) o.getKey();
            int weight = (int)(((Number) o.getValue()).floatValue() * 100);
            if (weight <= 0 || weight > 100)
              throw new IllegalArgumentException("Color weight lies outside (0, 100]: " + weight);
            totalWeight += weight;

            sb.append(QUERY_OPT_COLOR).append(QUERY_OPT_NAMEDELIM)
              .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
              .append(Utils.toHex(c.value, hexStrBuf))
              .append(QUERY_OPT_VALUEDELIM).append(weight)
              .append(QUERY_OPT_COLOR_SUFFIX).append(QUERY_SPACE)
              .append(QUERY_OPT_COLORGROUP).append(QUERY_OPT_NAMEDELIM)
              .append(c.groupName).append(QUERY_OPT_VALUEDELIM)
              .append(weight);
          }
          else
          {
            sb.append(urlEncode(o.getKey().toString())).append(QUERY_OPT_NAMEDELIM)
              .append(urlEncode(o.getValue().toString()));
          }
        }
        sb.append(')');

        if (totalWeight > 100)
          throw new IllegalArgumentException("Total color weight exceeds 100: " + totalWeight);
      }
    }

    return sb;
  }

  @Override
  public String toString()
  {
    return buildQueryString().toString();
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

  public static final String
    DEFAULT_URL_PATH = "http://chromatik.labs.exalead.com/searchphotos",
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
